package dev.marshall.hounded.display;

import dev.marshall.hounded.PlayerNames;
import dev.marshall.hounded.Ticks;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.DisplayMode;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.Messages;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.tracking.TrackingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Refreshes every player's display once a second. The mode is read each time, so {@code /hounded
 * reload} can switch between boss bar, scoreboard and none while a round is running.
 */
public final class HudService {
    private final Plugin plugin;
    private final GameSession session;
    private final TrackingService trackingService;
    private final ConfigService configService;
    private final Map<DisplayMode, Hud> huds;
    private BukkitTask refreshTask;

    public HudService(
            Plugin plugin, GameSession session, TrackingService trackingService, ConfigService configService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.session = Objects.requireNonNull(session, "session");
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
        this.configService = Objects.requireNonNull(configService, "configService");
        Server server = plugin.getServer();
        this.huds = Map.of(DisplayMode.BOSSBAR, new BossBarHud(server), DisplayMode.SCOREBOARD, new SidebarHud(server));
    }

    public void start() {
        refreshTask = plugin.getServer()
                .getScheduler()
                .runTaskTimer(plugin, this::refresh, Ticks.PER_SECOND, Ticks.PER_SECOND);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        huds.values().forEach(Hud::hideAll);
    }

    public void playerQuit(Player player) {
        huds.values().forEach(hud -> hud.hide(player));
    }

    private void refresh() {
        DisplayMode mode = configService.settings().display().mode();
        // A mode switched by reload leaves the previous display behind unless it is cleared here.
        huds.forEach((otherMode, hud) -> {
            if (otherMode != mode) {
                hud.hideAll();
            }
        });
        Hud active = huds.get(mode);
        if (active == null) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            HudFrame frame = HudComposer.compose(inputFor(player));
            if (frame.isHidden()) {
                active.hide(player);
            } else {
                active.show(player, render(frame));
            }
        }
    }

    private HudInput inputFor(Player player) {
        return new HudInput(
                session.state(),
                session.headstartRemaining(),
                session.headstartLength(),
                session.elapsedHuntTime(),
                session.roleOf(player.getUniqueId()),
                trackingService
                        .read(player)
                        .map(reading -> new HudInput.Tracking(
                                reading, PlayerNames.displayName(plugin.getServer(), reading.runner()))),
                configService.settings().display().showDistance());
    }

    private Hud.Rendered render(HudFrame frame) {
        Messages messages = configService.messages();
        List<Component> lines =
                frame.lines().stream().map(line -> render(messages, line)).toList();
        return new Hud.Rendered(
                messages.render(MessageKey.DISPLAY_TITLE),
                messages.render(MessageKey.DISPLAY_SEPARATOR),
                lines,
                frame.progress());
    }

    private static Component render(Messages messages, HudLine line) {
        List<TagResolver> placeholders = new ArrayList<>();
        line.values().forEach((name, value) -> placeholders.add(Placeholder.unparsed(name, value)));
        line.nested().forEach((name, key) -> placeholders.add(Placeholder.component(name, messages.render(key))));
        return messages.render(line.key(), placeholders.toArray(TagResolver[]::new));
    }
}
