package dev.marshall.hounded.game;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * One manhunt round and every state transition it can make. Pure Java so the whole state machine
 * is unit-testable; the plugin feeds it events (ticks, deaths, disconnects, dragon kill) and reacts to the
 * returned {@link TransitionResult}. Not thread-safe: call from the main thread only.
 */
public final class GameSession {
    private final Clock clock;
    private final Roster roster = new Roster();
    private final Set<UUID> eliminatedRunners = new HashSet<>();
    private final Map<UUID, Instant> rejoinDeadlines = new LinkedHashMap<>();

    private GameState state = GameState.LOBBY;
    private GameOutcome outcome;
    private Instant headstartEndsAt;
    private Instant runningSince;
    private Instant endedAt;

    public GameSession(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public GameState state() {
        return state;
    }

    /** Present only in {@link GameState#ENDED}. */
    public Optional<GameOutcome> outcome() {
        return Optional.ofNullable(outcome);
    }

    public Optional<Role> roleOf(UUID player) {
        return roster.roleOf(player);
    }

    public List<UUID> playersWith(Role role) {
        return roster.playersWith(role);
    }

    public TransitionResult assignRole(UUID player, Role role) {
        return changeRoles(() -> roster.assign(player, role));
    }

    /** Removes {@code role} from the player; rejected if they hold a different role or none. */
    public TransitionResult unassignRole(UUID player, Role role) {
        return changeRoles(() -> {
            if (roster.roleOf(player).filter(role::equals).isEmpty()) {
                return Optional.of(RejectionReason.NOT_IN_ROLE);
            }
            roster.unassign(player);
            return Optional.empty();
        });
    }

    public TransitionResult clearRole(Role role) {
        return changeRoles(() -> roster.clear(role));
    }

    /** Starts a round. A headstart of 0 skips {@link GameState#HEADSTART}. */
    public TransitionResult start(int headstartSeconds) {
        if (state != GameState.LOBBY) {
            return new TransitionResult.Rejected(RejectionReason.NOT_IN_LOBBY);
        }
        if (headstartSeconds < 0) {
            return new TransitionResult.Rejected(RejectionReason.NEGATIVE_HEADSTART);
        }
        if (!roster.hasAny(Role.RUNNER)) {
            return new TransitionResult.Rejected(RejectionReason.NO_RUNNERS);
        }
        if (!roster.hasAny(Role.HUNTER)) {
            return new TransitionResult.Rejected(RejectionReason.NO_HUNTERS);
        }
        if (headstartSeconds == 0) {
            return enterRunning(GameState.LOBBY);
        }
        headstartEndsAt = clock.instant().plusSeconds(headstartSeconds);
        return moveTo(GameState.HEADSTART);
    }

    /** Called periodically; ends the headstart once its time is up. */
    public TransitionResult tick() {
        if (state == GameState.HEADSTART && !clock.instant().isBefore(headstartEndsAt)) {
            return enterRunning(GameState.HEADSTART);
        }
        return new TransitionResult.Unchanged(state);
    }

    /** A runner is out for the round, by dying or by not returning in time. */
    public TransitionResult eliminateRunner(UUID player) {
        return whenRunnerInRound(player, () -> {
            eliminatedRunners.add(player);
            rejoinDeadlines.remove(player);
            if (remainingRunners().isEmpty()) {
                return end(GameOutcome.HUNTERS_WIN);
            }
            return new TransitionResult.Unchanged(state);
        });
    }

    /** The runner left the server; they stay in the round until {@code rejoinGrace} has passed. */
    public TransitionResult recordRunnerLeft(UUID player, Duration rejoinGrace) {
        if (rejoinGrace.isNegative()) {
            throw new IllegalArgumentException("rejoinGrace must not be negative");
        }
        return whenRunnerInRound(player, () -> {
            rejoinDeadlines.put(player, clock.instant().plus(rejoinGrace));
            return new TransitionResult.Unchanged(state);
        });
    }

    public TransitionResult recordRunnerReturned(UUID player) {
        if (rejoinDeadlines.remove(Objects.requireNonNull(player, "player")) == null) {
            return new TransitionResult.Rejected(RejectionReason.NOT_AWAITING_RETURN);
        }
        return new TransitionResult.Unchanged(state);
    }

    /** Runners who left and did not come back in time; the caller eliminates them. */
    public List<UUID> runnersPastRejoinDeadline() {
        Instant now = clock.instant();
        return rejoinDeadlines.entrySet().stream()
                .filter(entry -> !now.isBefore(entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public TransitionResult recordDragonKilled() {
        if (!state.isActive()) {
            return new TransitionResult.Rejected(RejectionReason.NOT_ACTIVE);
        }
        return end(GameOutcome.RUNNERS_WIN);
    }

    public TransitionResult stop() {
        if (!state.isActive()) {
            return new TransitionResult.Rejected(RejectionReason.NOT_ACTIVE);
        }
        return end(GameOutcome.STOPPED);
    }

    /** Returns to the lobby. Roles are kept so the same teams can play again. */
    public TransitionResult reset() {
        if (state != GameState.ENDED) {
            return new TransitionResult.Rejected(RejectionReason.NOT_ENDED);
        }
        eliminatedRunners.clear();
        rejoinDeadlines.clear();
        outcome = null;
        headstartEndsAt = null;
        runningSince = null;
        endedAt = null;
        return moveTo(GameState.LOBBY);
    }

    /** Runners that are still in the round, in assignment order. */
    public List<UUID> remainingRunners() {
        return roster.playersWith(Role.RUNNER).stream()
                .filter(runner -> !eliminatedRunners.contains(runner))
                .toList();
    }

    public boolean isEliminated(UUID player) {
        return eliminatedRunners.contains(player);
    }

    /** Time left in the headstart; zero outside {@link GameState#HEADSTART}. */
    public Duration headstartRemaining() {
        if (state != GameState.HEADSTART) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), headstartEndsAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /** Time since the hunt was released; zero before that. Frozen once the round ends. */
    public Duration elapsedHuntTime() {
        if (runningSince == null) {
            return Duration.ZERO;
        }
        Instant until = endedAt != null ? endedAt : clock.instant();
        return Duration.between(runningSince, until);
    }

    /** Runs {@code action} only for a runner who is still in an active round. */
    private TransitionResult whenRunnerInRound(UUID player, Supplier<TransitionResult> action) {
        return checkRunnerInRound(player)
                .<TransitionResult>map(TransitionResult.Rejected::new)
                .orElseGet(action);
    }

    private Optional<RejectionReason> checkRunnerInRound(UUID player) {
        Objects.requireNonNull(player, "player");
        if (!state.isActive()) {
            return Optional.of(RejectionReason.NOT_ACTIVE);
        }
        if (roster.roleOf(player).filter(Role.RUNNER::equals).isEmpty()) {
            return Optional.of(RejectionReason.NOT_A_RUNNER);
        }
        if (eliminatedRunners.contains(player)) {
            return Optional.of(RejectionReason.ALREADY_ELIMINATED);
        }
        return Optional.empty();
    }

    private TransitionResult changeRoles(Runnable change) {
        return changeRoles(() -> {
            change.run();
            return Optional.empty();
        });
    }

    /**
     * Roles are locked during a round because changing them would silently change who can win.
     *
     * @param change applies the change, or returns why it can't
     */
    private TransitionResult changeRoles(Supplier<Optional<RejectionReason>> change) {
        if (state.isActive()) {
            return new TransitionResult.Rejected(RejectionReason.ROLES_LOCKED);
        }
        return change.get()
                .<TransitionResult>map(TransitionResult.Rejected::new)
                .orElseGet(() -> new TransitionResult.Unchanged(state));
    }

    private TransitionResult enterRunning(GameState from) {
        runningSince = clock.instant();
        headstartEndsAt = null;
        state = GameState.RUNNING;
        return new TransitionResult.Changed(from, GameState.RUNNING);
    }

    private TransitionResult end(GameOutcome result) {
        outcome = result;
        endedAt = clock.instant();
        return moveTo(GameState.ENDED);
    }

    private TransitionResult moveTo(GameState target) {
        GameState from = state;
        state = target;
        return new TransitionResult.Changed(from, target);
    }
}
