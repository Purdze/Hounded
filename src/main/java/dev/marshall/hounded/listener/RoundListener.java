package dev.marshall.hounded.listener;

import dev.marshall.hounded.round.RoundService;
import java.util.Objects;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/** Feeds deaths into the round. The session itself ignores non-runners and inactive rounds. */
public final class RoundListener implements Listener {
    private final RoundService roundService;

    public RoundListener(RoundService roundService) {
        this.roundService = Objects.requireNonNull(roundService, "roundService");
    }

    // MONITOR + ignoreCancelled: only count deaths that other plugins let happen.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        roundService.recordRunnerDeath(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            roundService.recordDragonKilled();
        }
    }
}
