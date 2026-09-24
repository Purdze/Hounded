package dev.marshall.hounded.listener;

import dev.marshall.hounded.rules.RuleEnforcer;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;

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
    public void onMove(PlayerMoveEvent event) {
        PositionLock.holdIfFrozen(event, ruleEnforcer::isHeldByGaze);
    }
}
