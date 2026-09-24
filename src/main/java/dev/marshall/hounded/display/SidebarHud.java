package dev.marshall.hounded.display;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Shows the display in the scoreboard sidebar. Each player gets their own scoreboard because the
 * lines differ per player; the server's main scoreboard is given back when it is hidden.
 */
final class SidebarHud extends PerPlayerHud<Scoreboard> {
    private static final String OBJECTIVE = "hounded";
    private static final String LINE_ENTRY_PREFIX = "hounded-line-";

    SidebarHud(Server server) {
        super(server);
    }

    @Override
    public void show(Player player, Rendered frame) {
        Scoreboard board = shownTo.computeIfAbsent(player.getUniqueId(), ignored -> newBoard());
        Objective objective = Objects.requireNonNull(board.getObjective(OBJECTIVE));
        objective.displayName(frame.title());
        board.getEntries().forEach(board::resetScores);
        List<Component> lines = frame.lines();
        for (int line = 0; line < lines.size(); line++) {
            var score = objective.getScore(LINE_ENTRY_PREFIX + line);
            // Higher scores sit higher in the sidebar, so count down to keep the lines in order.
            score.setScore(lines.size() - line);
            score.customName(lines.get(line));
        }
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
    }

    @Override
    public void hide(Player player) {
        if (shownTo.remove(player.getUniqueId()) != null) {
            player.setScoreboard(server.getScoreboardManager().getMainScoreboard());
        }
    }

    private Scoreboard newBoard() {
        Scoreboard board = server.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE, Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        return board;
    }
}
