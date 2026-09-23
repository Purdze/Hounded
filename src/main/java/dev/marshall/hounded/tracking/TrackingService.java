package dev.marshall.hounded.tracking;

import dev.marshall.hounded.PlayerNames;
import dev.marshall.hounded.config.CompassUpdateMode;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Keeps hunters' compasses pointing at a runner, across dimensions. Runner positions and portal
 * exits are kept while a runner is offline, because the compass keeps pointing where they were
 * last seen. Main thread only.
 */
public final class TrackingService {
    private final GameSession session;
    private final TargetResolver resolver;
    private final CompassItem compassItem;
    private final ConfigService configService;
    private final Server server;
    private final PortalMemory portalMemory = new PortalMemory();
    private final Map<UUID, DimensionalPosition> lastSeenRunners = new HashMap<>();
    private final Map<UUID, Status> statusByHunter = new HashMap<>();

    /** What a hunter's compass is doing; a change is announced once instead of on every update. */
    private enum Status {
        TRACKING,
        NO_DATA
    }

    public TrackingService(
            GameSession session,
            TargetResolver resolver,
            CompassItem compassItem,
            ConfigService configService,
            Server server) {
        this.session = Objects.requireNonNull(session, "session");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.compassItem = Objects.requireNonNull(compassItem, "compassItem");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.server = Objects.requireNonNull(server, "server");
    }

    /** @return false if the player is not a hunter in an active round */
    public boolean giveCompass(Player hunter) {
        if (!hasRoleInRound(hunter, Role.HUNTER)) {
            return false;
        }
        hunter.getInventory()
                .addItem(compassItem.create(configService.messages().render(MessageKey.COMPASS_NAME)));
        MessageKey howToUse = configService.settings().compass().updateMode() == CompassUpdateMode.AUTO
                ? MessageKey.COMPASS_HOW_TO_USE_AUTO
                : MessageKey.COMPASS_HOW_TO_USE_MANUAL;
        selectedRunner().ifPresent(runner -> send(hunter, howToUse, runner));
        updateCompass(hunter, false);
        return true;
    }

    /**
     * Points the hunter's tracking compasses at their runner. Only while hunters are released, so
     * the compass gives nothing away during the headstart.
     *
     * @param announceStatus repeat the "no data" message even if nothing changed,
     *     because the hunter asked for an update
     */
    public void updateCompass(Player hunter, boolean announceStatus) {
        if (session.state() != GameState.RUNNING || !hasRoleInRound(hunter, Role.HUNTER)) {
            return;
        }
        // A running round always has a runner left; the last elimination ends it.
        Optional<UUID> runner = selectedRunner();
        if (runner.isEmpty()) {
            return;
        }
        Optional<Position> target = Dimensions.of(hunter.getWorld())
                .flatMap(dimension -> positionOf(resolver.resolve(new TrackingSnapshot(
                        dimension, runnerLocation(runner.get()), portalMemory.exitsOf(runner.get())))));
        Location pointAt = target.map(
                        position -> new Location(hunter.getWorld(), position.x(), position.y(), position.z()))
                .orElseGet(() -> hunter.getWorld().getSpawnLocation());
        forEachTrackingCompass(hunter.getInventory(), compass -> compassItem.pointAt(compass, pointAt));
        report(hunter, target.isPresent() ? Status.TRACKING : Status.NO_DATA, announceStatus, runner.get());
    }

    /** A runner changing worlds left through a portal (or was teleported) at {@code from}. */
    public void recordWorldChange(Player player, Location from) {
        if (hasRoleInRound(player, Role.RUNNER)) {
            Dimensions.positionOf(from).ifPresent(exit -> portalMemory.recordExit(player.getUniqueId(), exit));
        }
    }

    public void recordQuit(Player player) {
        if (hasRoleInRound(player, Role.RUNNER)) {
            Dimensions.positionOf(player.getLocation())
                    .ifPresent(position -> lastSeenRunners.put(player.getUniqueId(), position));
        }
        statusByHunter.remove(player.getUniqueId());
    }

    public void stop() {
        portalMemory.clear();
        lastSeenRunners.clear();
        statusByHunter.clear();
    }

    // TODO(compass part 2): per-hunter selection with left-click cycling.
    private Optional<UUID> selectedRunner() {
        return session.remainingRunners().stream().findFirst();
    }

    private Optional<DimensionalPosition> runnerLocation(UUID runner) {
        Player online = server.getPlayer(runner);
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

    /**
     * Rewrites a slot only when the compass changed, so the item isn't re-sent needlessly.
     *
     * @param change modifies a compass and returns whether it did
     */
    private void forEachTrackingCompass(PlayerInventory inventory, Predicate<ItemStack> change) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (compassItem.isTrackingCompass(item) && change.test(item)) {
                inventory.setItem(slot, item);
            }
        }
    }

    private void report(Player hunter, Status status, boolean announceStatus, UUID runner) {
        Status previous = statusByHunter.put(hunter.getUniqueId(), status);
        if (status == Status.NO_DATA && (announceStatus || previous != status)) {
            send(hunter, MessageKey.COMPASS_NO_DATA, runner);
        }
    }

    private void send(Player hunter, MessageKey key, UUID runner) {
        TagResolver runnerName = Placeholder.unparsed(PlaceholderNames.RUNNER, PlayerNames.displayName(server, runner));
        hunter.sendMessage(configService.messages().chat(key, runnerName));
    }

    private boolean hasRoleInRound(Player player, Role role) {
        return session.state().isActive() && session.hasRole(player.getUniqueId(), role);
    }
}
