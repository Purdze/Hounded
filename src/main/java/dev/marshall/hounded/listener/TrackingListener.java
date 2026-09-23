package dev.marshall.hounded.listener;

import dev.marshall.hounded.tracking.CompassItem;
import dev.marshall.hounded.tracking.TrackingService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Feeds world changes, quits and compass clicks into tracking. */
public final class TrackingListener implements Listener {
    private final TrackingService trackingService;
    private final CompassItem compassItem;

    public TrackingListener(TrackingService trackingService, CompassItem compassItem) {
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
        this.compassItem = Objects.requireNonNull(compassItem, "compassItem");
    }

    // Portals are teleports too, so this covers the Nether, the End and /tp between worlds.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            trackingService.recordWorldChange(event.getPlayer(), event.getFrom());
        }
    }

    // Right-clicking air arrives pre-cancelled, so cancelled events are not skipped.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && compassItem.isTrackingCompass(event.getItem())) {
            trackingService.updateCompass(event.getPlayer(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        trackingService.recordQuit(event.getPlayer());
    }
}
