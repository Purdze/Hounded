package dev.marshall.hounded.onboarding;

import dev.marshall.hounded.Permissions;
import dev.marshall.hounded.Ticks;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Tells admins how to start their first round, until one has been played. */
public final class QuickStartGuide {
    // Late enough not to be buried under join messages and the message of the day.
    static final long DELAY_TICKS = 2 * Ticks.PER_SECOND;

    private final Plugin plugin;
    private final ConfigService configService;
    private final FirstRoundMarker firstRoundMarker;

    public QuickStartGuide(Plugin plugin, ConfigService configService, FirstRoundMarker firstRoundMarker) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.firstRoundMarker = Objects.requireNonNull(firstRoundMarker, "firstRoundMarker");
    }

    public void playerJoined(Player player) {
        if (!configService.settings().showQuickStartGuide()
                || firstRoundMarker.hasPlayedARound()
                || !player.hasPermission(Permissions.ADMIN)) {
            return;
        }
        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {
                            if (player.isOnline()) {
                                player.sendMessage(configService.messages().chat(MessageKey.QUICK_START_GUIDE));
                            }
                        },
                        DELAY_TICKS);
    }
}
