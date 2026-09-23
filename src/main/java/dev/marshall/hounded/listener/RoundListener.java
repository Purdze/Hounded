package dev.marshall.hounded.listener;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import dev.marshall.hounded.round.RoundService;
import java.util.Objects;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Feeds deaths, quits, joins and respawns into the round. The session ignores non-runners and inactive rounds. */
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        roundService.playerLeft(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        roundService.playerJoined(event.getPlayer());
    }

    // After the respawn has completed; changing gamemode during PlayerRespawnEvent is unreliable.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        roundService.playerRespawned(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            roundService.recordDragonKilled();
        }
    }
}
