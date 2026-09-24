package dev.marshall.hounded.tracking;

import dev.marshall.hounded.PlayerNames;
import dev.marshall.hounded.config.CompassUpdateMode;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.config.Settings;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Keeps hunters' compasses pointing at their chosen runner, across dimensions. Runner positions and
 * portal exits are kept while a runner is offline, because the compass keeps pointing where they
 * were last seen; everything is forgotten when the round ends. Main thread only.
 */
public final class TrackingService {
    private final Plugin plugin;
    private final GameSession session;
    private final TargetResolver resolver;
    private final CompassItem compassItem;
    private final ConfigService configService;
    private final PortalMemory portalMemory = new PortalMemory();
    private final Map<UUID, DimensionalPosition> lastSeenRunners = new HashMap<>();
    private final Map<UUID, UUID> chosenRunnerByHunter = new HashMap<>();
    private final Map<UUID, TrackingReading.Kind> lastKindByHunter = new HashMap<>();
    private BukkitTask updateTask;
    private long ticksSinceUpdate;

    /** A reading plus the position the compass should point at, if any. */
    private record Resolution(TrackingReading reading, Optional<Position> target) {}

    public TrackingService(
            Plugin plugin,
            GameSession session,
            TargetResolver resolver,
            CompassItem compassItem,
            ConfigService configService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.session = Objects.requireNonNull(session, "session");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.compassItem = Objects.requireNonNull(compassItem, "compassItem");
        this.configService = Objects.requireNonNull(configService, "configService");
    }

    /**
     * Runs every tick and counts up to the configured interval itself, so a changed interval takes
     * effect on {@code /hounded reload} without rescheduling.
     */
    public void start() {
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::onTick, 1L, 1L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        forgetRound();
    }

    /** The runner this hunter's compass follows. */
    public Optional<UUID> trackedRunner(Player hunter) {
        return RunnerCycle.current(
                session.remainingRunners(), Optional.ofNullable(chosenRunnerByHunter.get(hunter.getUniqueId())));
    }

    /** Switches the hunter to the next runner still in the round. */
    public void cycleRunner(Player hunter) {
        if (!session.isPlaying(hunter.getUniqueId(), Role.HUNTER)) {
            return;
        }
        RunnerCycle.next(session.remainingRunners(), trackedRunner(hunter)).ifPresent(runner -> {
            chosenRunnerByHunter.put(hunter.getUniqueId(), runner);
            send(hunter, MessageKey.COMPASS_NOW_TRACKING, runner);
            updateCompass(hunter, false);
        });
    }

    /**
     * What the hunter's compass shows right now. Empty outside the hunt: during the headstart the
     * compass gives nothing away.
     */
    public Optional<TrackingReading> read(Player hunter) {
        return resolve(hunter).map(Resolution::reading);
    }

    /**
     * Points the hunter's tracking compasses at their runner.
     *
     * @param announceStatus repeat a status message even if nothing changed, because the hunter
     *     asked for an update
     */
    public void updateCompass(Player hunter, boolean announceStatus) {
        resolve(hunter).ifPresent(resolution -> {
            if (resolution.reading().kind() == TrackingReading.Kind.DISABLED) {
                compassItem.updateAll(hunter.getInventory(), compassItem::clearTarget);
            } else {
                Location pointAt = resolution
                        .target()
                        .map(position -> toLocation(hunter, position))
                        .orElseGet(() -> hunter.getWorld().getSpawnLocation());
                compassItem.updateAll(hunter.getInventory(), compass -> compassItem.pointAt(compass, pointAt));
            }
            report(hunter, resolution.reading(), announceStatus);
        });
    }

    /** A runner changing worlds left through a portal (or was teleported) at {@code from}. */
    public void recordWorldChange(Player player, Location from) {
        if (session.isPlaying(player.getUniqueId(), Role.RUNNER)) {
            Dimensions.positionOf(from).ifPresent(exit -> portalMemory.recordExit(player.getUniqueId(), exit));
        }
    }

    public void recordQuit(Player player) {
        if (session.isPlaying(player.getUniqueId(), Role.RUNNER)) {
            Dimensions.positionOf(player.getLocation())
                    .ifPresent(position -> lastSeenRunners.put(player.getUniqueId(), position));
        }
        chosenRunnerByHunter.remove(player.getUniqueId());
        lastKindByHunter.remove(player.getUniqueId());
    }

    public void forgetRound() {
        portalMemory.clear();
        lastSeenRunners.clear();
        chosenRunnerByHunter.clear();
        lastKindByHunter.clear();
    }

    /** Sends a compass message with {@code <runner>} filled in. */
    void send(Player hunter, MessageKey key, UUID runner) {
        TagResolver runnerName =
                Placeholder.unparsed(PlaceholderNames.RUNNER, PlayerNames.displayName(plugin.getServer(), runner));
        hunter.sendMessage(configService.messages().chat(key, runnerName));
    }

    private void onTick() {
        Settings.Compass settings = configService.settings().compass();
        if (settings.updateMode() != CompassUpdateMode.AUTO || ++ticksSinceUpdate < settings.updateIntervalTicks()) {
            return;
        }
        ticksSinceUpdate = 0;
        plugin.getServer().getOnlinePlayers().forEach(player -> updateCompass(player, false));
    }

    private Optional<DimensionalPosition> runnerLocation(UUID runner) {
        Player online = plugin.getServer().getPlayer(runner);
        if (online == null) {
            return Optional.ofNullable(lastSeenRunners.get(runner));
        }
        Optional<DimensionalPosition> now = Dimensions.positionOf(online.getLocation());
        now.ifPresent(position -> lastSeenRunners.put(runner, position));
        return now;
    }

    /** Problems are announced once when they start, or again when the hunter asks for an update. */
    private void report(Player hunter, TrackingReading reading, boolean announceStatus) {
        TrackingReading.Kind previous = lastKindByHunter.put(hunter.getUniqueId(), reading.kind());
        if (!announceStatus && previous == reading.kind()) {
            return;
        }
        switch (reading.kind()) {
            case NO_DATA -> send(hunter, MessageKey.COMPASS_NO_DATA, reading.runner());
            case DISABLED -> send(hunter, MessageKey.COMPASS_DISABLED_IN_NETHER, reading.runner());
            case RUNNER, PORTAL -> {}
        }
    }

    private Optional<Resolution> resolve(Player hunter) {
        if (session.state() != GameState.RUNNING || !session.isPlaying(hunter.getUniqueId(), Role.HUNTER)) {
            return Optional.empty();
        }
        // A running round always has a runner left; the last elimination ends it.
        Optional<UUID> tracked = trackedRunner(hunter);
        if (tracked.isEmpty()) {
            return Optional.empty();
        }
        UUID runner = tracked.get();
        Optional<DimensionalPosition> runnerAt = runnerLocation(runner);
        Optional<Dimension> runnerDimension = runnerAt.map(DimensionalPosition::dimension);
        Optional<DimensionalPosition> hunterAt = Dimensions.positionOf(hunter.getLocation());
        if (hunterAt.isEmpty()) {
            return Optional.of(noTarget(runner, TrackingReading.Kind.NO_DATA, runnerDimension));
        }
        if (hunterAt.get().dimension() == Dimension.NETHER
                && configService.settings().compass().disableInNetherForHunters()) {
            return Optional.of(noTarget(runner, TrackingReading.Kind.DISABLED, runnerDimension));
        }
        CompassTarget target = resolver.resolve(
                new TrackingSnapshot(hunterAt.get().dimension(), runnerAt, portalMemory.exitsOf(runner)));
        return Optional.of(
                switch (target) {
                    case CompassTarget.Runner found ->
                        towards(runner, TrackingReading.Kind.RUNNER, runnerDimension, hunterAt.get(), found.position());
                    case CompassTarget.LastPortal portal ->
                        towards(
                                runner,
                                TrackingReading.Kind.PORTAL,
                                runnerDimension,
                                hunterAt.get(),
                                portal.position());
                    case CompassTarget.NoData ignored ->
                        noTarget(runner, TrackingReading.Kind.NO_DATA, runnerDimension);
                });
    }

    private static Resolution towards(
            UUID runner,
            TrackingReading.Kind kind,
            Optional<Dimension> runnerDimension,
            DimensionalPosition hunterAt,
            Position target) {
        int distance = (int) Math.round(hunterAt.position().horizontalDistanceTo(target));
        return new Resolution(
                new TrackingReading(runner, kind, runnerDimension, OptionalInt.of(distance)), Optional.of(target));
    }

    private static Resolution noTarget(UUID runner, TrackingReading.Kind kind, Optional<Dimension> runnerDimension) {
        return new Resolution(
                new TrackingReading(runner, kind, runnerDimension, OptionalInt.empty()), Optional.empty());
    }

    private static Location toLocation(Player hunter, Position position) {
        return new Location(hunter.getWorld(), position.x(), position.y(), position.z());
    }
}
