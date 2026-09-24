package dev.marshall.hounded.display;

import java.util.Optional;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** Shows the display as a single boss bar per player, with the lines joined into one title. */
final class BossBarHud extends PerPlayerHud<BossBar> {

    BossBarHud(Server server) {
        super(server);
    }

    @Override
    public void show(Player player, Rendered frame) {
        BossBar bar = shownTo.computeIfAbsent(
                player.getUniqueId(),
                ignored -> BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
        bar.name(Component.join(JoinConfiguration.separator(frame.separator()), frame.lines()));
        bar.progress(frame.progress());
        player.showBossBar(bar);
    }

    @Override
    public void hide(Player player) {
        Optional.ofNullable(shownTo.remove(player.getUniqueId())).ifPresent(player::hideBossBar);
    }
}
