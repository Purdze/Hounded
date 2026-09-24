package dev.marshall.hounded.display;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** One way of showing the display: a boss bar or a scoreboard sidebar. */
interface Hud {

    /** A frame with its text already rendered from messages.yml. */
    record Rendered(Component title, Component separator, List<Component> lines, float progress) {}

    void show(Player player, Rendered frame);

    void hide(Player player);

    /** Hides the display from every player who has it and forgets them. */
    void hideAll();
}
