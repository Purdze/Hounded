package dev.marshall.hounded.listener;

import dev.marshall.hounded.round.HeadstartHold;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/** Stops frozen hunters from doing anything during the headstart, and from being hurt. */
public final class HeadstartListener implements Listener {
    private final HeadstartHold headstartHold;

    public HeadstartListener(HeadstartHold headstartHold) {
        this.headstartHold = Objects.requireNonNull(headstartHold, "headstartHold");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        PositionLock.holdIfFrozen(event, headstartHold::isFrozen);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        cancelIfFrozen(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        cancelIfFrozen(event, event.getPlayer());
    }

    // Interact events are often pre-cancelled (e.g. clicking air), so don't skip cancelled ones.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        cancelIfFrozen(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        cancelIfFrozen(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        cancelIfFrozen(event, event.getDamager());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        cancelIfFrozen(event, event.getEntity());
    }

    private void cancelIfFrozen(Cancellable event, Entity entity) {
        if (entity instanceof Player player && headstartHold.isFrozen(player)) {
            event.setCancelled(true);
        }
    }
}
