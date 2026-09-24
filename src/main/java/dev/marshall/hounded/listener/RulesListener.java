package dev.marshall.hounded.listener;

import dev.marshall.hounded.rules.RuleEnforcer;
import java.util.Objects;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/** Applies the optional round rules to attacks and to hunters' movement. */
public final class RulesListener implements Listener {
    private final RuleEnforcer ruleEnforcer;

    public RulesListener(RuleEnforcer ruleEnforcer) {
        this.ruleEnforcer = Objects.requireNonNull(ruleEnforcer, "ruleEnforcer");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (ruleEnforcer.blocksDamage(event.getDamager(), event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        for (LivingEntity victim : event.getAffectedEntities()) {
            if (ruleEnforcer.blocksSplash(potion, victim)) {
                event.setIntensity(victim, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLingeringCloud(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        event.getAffectedEntities().removeIf(victim -> ruleEnforcer.blocksCloud(cloud, victim));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Freeze.holdPositionIfFrozen(event, ruleEnforcer::isHeldByGaze);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        cancelIfHeldByGaze(event, event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        cancelIfHeldByGaze(event, event.getEntered());
    }

    /** A rider would otherwise escape the gaze by steering, since walking is what gets held. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleMove(VehicleMoveEvent event) {
        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (passenger instanceof Player rider && ruleEnforcer.isHeldByGaze(rider)) {
                rider.leaveVehicle();
            }
        }
    }

    private void cancelIfHeldByGaze(Cancellable event, Entity actor) {
        Freeze.cancelIfFrozen(event, actor, ruleEnforcer::isHeldByGaze);
    }
}
