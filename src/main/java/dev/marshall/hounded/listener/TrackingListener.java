package dev.marshall.hounded.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import dev.marshall.hounded.tracking.CompassHandout;
import dev.marshall.hounded.tracking.CompassItem;
import dev.marshall.hounded.tracking.TrackingService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Feeds world changes, clicks, joins, deaths and drops into compass tracking. */
public final class TrackingListener implements Listener {
    private final TrackingService trackingService;
    private final CompassHandout compassHandout;
    private final CompassItem compassItem;

    public TrackingListener(TrackingService trackingService, CompassHandout compassHandout, CompassItem compassItem) {
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
        this.compassHandout = Objects.requireNonNull(compassHandout, "compassHandout");
        this.compassItem = Objects.requireNonNull(compassItem, "compassItem");
    }

    // Portals are teleports too, so this covers the Nether, the End and /tp between worlds.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            trackingService.recordWorldChange(event.getPlayer(), event.getFrom());
        }
    }

    // Clicking air arrives pre-cancelled, so cancelled events are not skipped. Left-click is
    // cancelled so switching runner does not also start breaking a block.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!compassItem.isTrackingCompass(event.getItem())) {
            return;
        }
        if (event.getAction().isLeftClick()) {
            event.setCancelled(true);
            trackingService.cycleRunner(event.getPlayer());
        } else if (event.getAction().isRightClick()) {
            trackingService.updateCompass(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (compassItem.isTrackingCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        compassHandout.keepOutOfDrops(event.getDrops());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerPostRespawnEvent event) {
        compassHandout.playerRespawned(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        compassHandout.playerJoined(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        trackingService.recordQuit(event.getPlayer());
    }
}
