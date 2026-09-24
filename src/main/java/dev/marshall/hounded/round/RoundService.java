package dev.marshall.hounded.round;

import dev.marshall.hounded.PlayerNames;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameOutcome;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.TransitionResult;
import dev.marshall.hounded.tracking.CompassHandout;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Turns {@link GameSession} results into server effects: broadcasts, the round timer, spectator
 * mode for eliminated runners, holding hunters during the headstart and the reset after a round ends. Commands and listeners go through
 * here so they stay thin. Main thread only.
 */
public final class RoundService {
    private final Plugin plugin;
    private final GameSession session;
    private final ConfigService configService;
    private final HeadstartHold headstartHold;
    private final CompassHandout compassHandout;
    private final SpectatorSwitcher spectators;
    private BukkitTask roundTask;

    public RoundService(
            Plugin plugin,
            GameSession session,
            ConfigService configService,
            HeadstartHold headstartHold,
            CompassHandout compassHandout) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.session = Objects.requireNonNull(session, "session");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.headstartHold = Objects.requireNonNull(headstartHold, "headstartHold");
        this.compassHandout = Objects.requireNonNull(compassHandout, "compassHandout");
        this.spectators = new SpectatorSwitcher(plugin.getServer());
    }

    public TransitionResult start(int headstartSeconds) {
        TransitionResult result = session.start(headstartSeconds);
        if (result instanceof TransitionResult.Changed changed) {
            if (changed.to() == GameState.HEADSTART) {
                broadcast(MessageKey.START_HEADSTART, seconds(headstartSeconds));
                headstartHold.holdOnlineHunters();
            } else {
                broadcast(MessageKey.START_RELEASED);
            }
            compassHandout.roundStarted();
            roundTask = plugin.getServer()
                    .getScheduler()
                    .runTaskTimer(plugin, this::tickRound, Ticks.PER_SECOND, Ticks.PER_SECOND);
        }
        return result;
    }

    public TransitionResult stop() {
        return finishIfEnded(session.stop());
    }

    public TransitionResult recordRunnerDeath(UUID player) {
        TransitionResult result = session.eliminateRunner(player);
        if (result instanceof TransitionResult.Unchanged) {
            broadcast(MessageKey.ROUND_RUNNER_ELIMINATED, playerName(player));
        }
        return finishIfEnded(result);
    }

    public TransitionResult recordDragonKilled() {
        return finishIfEnded(session.recordDragonKilled());
    }

    public void playerLeft(UUID player) {
        int graceSeconds = configService.settings().rules().runnerRejoinGraceSeconds();
        TransitionResult result = session.recordRunnerLeft(player, Duration.ofSeconds(graceSeconds));
        // With no grace, the timeout message on the next tick says it all.
        if (result instanceof TransitionResult.Unchanged && graceSeconds > 0) {
            broadcast(MessageKey.ROUND_RUNNER_LEFT, playerName(player), seconds(graceSeconds));
        }
    }

    public void playerJoined(Player player) {
        spectators.restoreIfPending(player);
        headstartHold.hold(player);
        if (session.recordRunnerReturned(player.getUniqueId()) instanceof TransitionResult.Unchanged) {
            broadcast(MessageKey.ROUND_RUNNER_RETURNED, playerName(player.getUniqueId()));
        }
        spectateIfEliminated(player);
    }

    public void playerRespawned(Player player) {
        spectateIfEliminated(player);
    }

    public void shutdown() {
        cancelRoundTask();
        headstartHold.releaseAll();
        spectators.restoreAll();
    }

    private void tickRound() {
        if (session.tick() instanceof TransitionResult.Changed) {
            headstartHold.releaseAll();
            broadcast(MessageKey.START_RELEASED);
        }
        for (UUID runner : session.runnersPastRejoinDeadline()) {
            broadcast(MessageKey.ROUND_RUNNER_TIMED_OUT, playerName(runner));
            finishIfEnded(session.eliminateRunner(runner));
        }
    }

    private void spectateIfEliminated(Player player) {
        if (session.isEliminated(player.getUniqueId())
                && configService.settings().rules().eliminatedRunnersSpectate()) {
            spectators.makeSpectator(player);
        }
    }

    private TransitionResult finishIfEnded(TransitionResult result) {
        if (session.state() == GameState.ENDED) {
            cancelRoundTask();
            headstartHold.releaseAll();
            compassHandout.roundEnded();
            announce(session.outcome().orElseThrow());
            spectators.restoreAll();
            session.reset();
        }
        return result;
    }

    private void announce(GameOutcome outcome) {
        MessageKey key = switch (outcome) {
            case RUNNERS_WIN -> MessageKey.WIN_RUNNERS;
            case HUNTERS_WIN -> MessageKey.WIN_HUNTERS;
            case STOPPED -> MessageKey.STOP_STOPPED;
        };
        broadcast(
                key, Placeholder.unparsed(PlaceholderNames.TIME, HuntTimeFormatter.format(session.elapsedHuntTime())));
    }

    private void cancelRoundTask() {
        if (roundTask != null) {
            roundTask.cancel();
            roundTask = null;
        }
    }

    private TagResolver playerName(UUID player) {
        return Placeholder.unparsed(PlaceholderNames.PLAYER, PlayerNames.displayName(plugin.getServer(), player));
    }

    private static TagResolver seconds(int seconds) {
        return Placeholder.unparsed(PlaceholderNames.SECONDS, Integer.toString(seconds));
    }

    private void broadcast(MessageKey key, TagResolver... placeholders) {
        plugin.getServer().broadcast(configService.messages().chat(key, placeholders));
    }
}
