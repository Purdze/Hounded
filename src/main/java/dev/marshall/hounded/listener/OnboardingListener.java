package dev.marshall.hounded.listener;

import dev.marshall.hounded.onboarding.QuickStartGuide;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Offers first-run help to admins as they join. */
public final class OnboardingListener implements Listener {
    private final QuickStartGuide quickStartGuide;

    public OnboardingListener(QuickStartGuide quickStartGuide) {
        this.quickStartGuide = Objects.requireNonNull(quickStartGuide, "quickStartGuide");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        quickStartGuide.playerJoined(event.getPlayer());
    }
}
