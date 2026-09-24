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
    private final Map<UUID, Status> statusByHunter = new HashMap<>();
    private BukkitTask updateTask;
    private long ticksSinceUpdate;

    /** What a hunter's compass is doing; a change is announced once instead of on every update. */
    private enum Status {
        TRACKING,
        NO_DATA,
        DISABLED
    }

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
     * Points the hunter's tracking compasses at their runner. Only while hunters are released, so
     * the compass gives nothing away during the headstart.
     *
     * @param announceStatus repeat a status message even if nothing changed, because the hunter
     *     asked for an update
     */
    public void updateCompass(Player hunter, boolean announceStatus) {
        if (session.state() != GameState.RUNNING || !session.isPlaying(hunter.getUniqueId(), Role.HUNTER)) {
            return;
        }
        // A running round always has a runner left; the last elimination ends it.
        Optional<UUID> runner = trackedRunner(hunter);
        if (runner.isEmpty()) {
            return;
        }
        Optional<Dimension> dimension = Dimensions.of(hunter.getWorld());
        if (dimension.equals(Optional.of(Dimension.NETHER))
                && configService.settings().compass().disableInNetherForHunters()) {
            compassItem.updateAll(hunter.getInventory(), compassItem::clearTarget);
            report(hunter, Status.DISABLED, announceStatus, runner.get());
            return;
        }
        Optional<Position> target = dimension.flatMap(here -> positionOf(resolver.resolve(
                new TrackingSnapshot(here, runnerLocation(runner.get()), portalMemory.exitsOf(runner.get())))));
        Location pointAt = target.map(
                        position -> new Location(hunter.getWorld(), position.x(), position.y(), position.z()))
                .orElseGet(() -> hunter.getWorld().getSpawnLocation());
        compassItem.updateAll(hunter.getInventory(), compass -> compassItem.pointAt(compass, pointAt));
        report(hunter, target.isPresent() ? Status.TRACKING : Status.NO_DATA, announceStatus, runner.get());
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
        statusByHunter.remove(player.getUniqueId());
    }

    public void forgetRound() {
        portalMemory.clear();
        lastSeenRunners.clear();
        chosenRunnerByHunter.clear();
        statusByHunter.clear();
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

    private static Optional<Position> positionOf(CompassTarget target) {
        return switch (target) {
            case CompassTarget.Runner runner -> Optional.of(runner.position());
            case CompassTarget.LastPortal portal -> Optional.of(portal.position());
            case CompassTarget.NoData ignored -> Optional.empty();
        };
    }

    private void report(Player hunter, Status status, boolean announceStatus, UUID runner) {
        Status previous = statusByHunter.put(hunter.getUniqueId(), status);
        if (status == Status.TRACKING || (!announceStatus && previous == status)) {
            return;
        }
        MessageKey key = status == Status.NO_DATA ? MessageKey.COMPASS_NO_DATA : MessageKey.COMPASS_DISABLED_IN_NETHER;
        send(hunter, key, runner);
    }
}
