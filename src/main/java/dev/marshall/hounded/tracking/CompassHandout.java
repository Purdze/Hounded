package dev.marshall.hounded.tracking;

import dev.marshall.hounded.config.CompassUpdateMode;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import java.util.List;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Hands tracking compasses to hunters and takes them back when the round ends. A hunter who was
 * offline at the end loses theirs when they next join, so nothing has to be remembered for them.
 */
public final class CompassHandout {
    private final GameSession session;
    private final TrackingService tracking;
    private final CompassItem compassItem;
    private final ConfigService configService;
    private final Server server;

    public CompassHandout(
            GameSession session,
            TrackingService tracking,
            CompassItem compassItem,
            ConfigService configService,
            Server server) {
        this.session = Objects.requireNonNull(session, "session");
        this.tracking = Objects.requireNonNull(tracking, "tracking");
        this.compassItem = Objects.requireNonNull(compassItem, "compassItem");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.server = Objects.requireNonNull(server, "server");
    }

    /**
     * Gives a compass unless the hunter already carries one, and explains how to use it.
     *
     * @return false if the player is not a hunter in a round
     */
    public boolean give(Player hunter) {
        if (!session.isPlaying(hunter.getUniqueId(), Role.HUNTER)) {
            return false;
        }
        if (!compassItem.isIn(hunter.getInventory())) {
            hunter.getInventory()
                    .addItem(compassItem.create(configService.messages().render(MessageKey.COMPASS_NAME)));
        }
        MessageKey howToUse = configService.settings().compass().updateMode() == CompassUpdateMode.AUTO
                ? MessageKey.COMPASS_HOW_TO_USE_AUTO
                : MessageKey.COMPASS_HOW_TO_USE_MANUAL;
        tracking.trackedRunner(hunter).ifPresent(runner -> tracking.send(hunter, howToUse, runner));
        tracking.updateCompass(hunter, false);
        return true;
    }

    public void roundStarted() {
        server.getOnlinePlayers().forEach(this::give);
    }

    public void roundEnded() {
        server.getOnlinePlayers().forEach(player -> compassItem.removeAll(player.getInventory()));
        tracking.forgetRound();
    }

    /** Hunters get their compass back; anyone else loses a leftover one from an earlier round. */
    public void playerJoined(Player player) {
        if (!give(player)) {
            compassItem.removeAll(player.getInventory());
        }
    }

    public void playerRespawned(Player player) {
        give(player);
    }

    /** The compass is re-issued on respawn, so it must not also be left on the ground. */
    public void keepOutOfDrops(List<ItemStack> drops) {
        drops.removeIf(compassItem::isTrackingCompass);
    }
}
