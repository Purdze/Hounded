package dev.marshall.hounded.listener;

import dev.marshall.hounded.Permissions;
import dev.marshall.hounded.round.HeadstartHold;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
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
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleEnterEvent;

/** Stops frozen hunters from doing anything during the headstart, and from being hurt. */
public final class HeadstartListener implements Listener {
    /** Ways out of the freeze. Dismounting stays allowed, since the hold itself dismounts hunters. */
    private static final Set<TeleportCause> ESCAPE_TELEPORTS = EnumSet.of(
            TeleportCause.COMMAND,
            TeleportCause.PLUGIN,
            TeleportCause.ENDER_PEARL,
            TeleportCause.CHORUS_FRUIT,
            TeleportCause.SPECTATE);

    private final HeadstartHold headstartHold;

    public HeadstartListener(HeadstartHold headstartHold) {
        this.headstartHold = Objects.requireNonNull(headstartHold, "headstartHold");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Freeze.holdPositionIfFrozen(event, headstartHold::isFrozen);
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
    public void onInteractWithEntity(PlayerInteractEntityEvent event) {
        cancelIfFrozen(event, event.getPlayer());
    }

    // Armor stands raise this separately, with its own handler list.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        cancelIfFrozen(event, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        cancelIfFrozen(event, event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        cancelIfFrozen(event, event.getEntered());
    }

    /** Admins may still move themselves, for example to fix a bad spawn. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (ESCAPE_TELEPORTS.contains(event.getCause()) && !player.hasPermission(Permissions.ADMIN)) {
            cancelIfFrozen(event, player);
        }
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

    private void cancelIfFrozen(Cancellable event, Entity actor) {
        Freeze.cancelIfFrozen(event, actor, headstartHold::isFrozen);
    }
}
