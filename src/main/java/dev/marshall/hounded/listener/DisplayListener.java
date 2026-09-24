package dev.marshall.hounded.listener;

import dev.marshall.hounded.display.HudService;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Forgets a player's display when they leave. */
public final class DisplayListener implements Listener {
    private final HudService hudService;

    public DisplayListener(HudService hudService) {
        this.hudService = Objects.requireNonNull(hudService, "hudService");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        hudService.playerQuit(event.getPlayer());
    }
}
