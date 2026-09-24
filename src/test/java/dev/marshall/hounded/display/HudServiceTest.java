package dev.marshall.hounded.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The display through a real round. Only the boss bar is covered here: MockBukkit doesn't simulate
 * the scoreboard number format the sidebar needs, so the sidebar is checked in-game.
 */
class HudServiceTest {
    private static final long ONE_REFRESH = 20;

    private PluginFixture fixture;
    private PlayerMock hunter;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        World world = fixture.addWorld("world", World.Environment.NORMAL);
        hunter = fixture.addAdmin("Hunter");
        runner = fixture.server().addPlayer("Runner");
        hunter.teleport(new Location(world, 0, 64, 0));
        runner.teleport(new Location(world, 30, 64, 40));
        fixture.startRound(hunter, 0, runner);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private void refresh() {
        fixture.server().getScheduler().performTicks(ONE_REFRESH);
    }

    private static Optional<String> bossBarText(PlayerMock player) {
        return StreamSupport.stream(player.activeBossBars().spliterator(), false)
                .map(BossBar::name)
                .map(PluginFixture::plain)
                .findFirst();
    }

    private String timerLine() {
        return fixture.text(
                MessageKey.DISPLAY_TIMER, Placeholder.unparsed(PlaceholderNames.TIME, PluginFixture.INSTANT_HUNT_TIME));
    }

    private String distanceLine() {
        return fixture.text(
                MessageKey.DISPLAY_DISTANCE,
                Placeholder.unparsed(PlaceholderNames.RUNNER, runner.getName()),
                Placeholder.unparsed(PlaceholderNames.DISTANCE, "50"));
    }

    @Test
    void hunterSeesTimerAndDistanceInTheBossBar() {
        refresh();

        assertEquals(
                Optional.of(timerLine() + fixture.text(MessageKey.DISPLAY_SEPARATOR) + distanceLine()),
                bossBarText(hunter));
    }

    @Test
    void runnerOnlySeesTheTimer() {
        refresh();

        assertEquals(Optional.of(timerLine()), bossBarText(runner));
    }

    @Test
    void displayDisappearsWhenTheRoundEnds() {
        refresh();

        hunter.performCommand("hounded stop");
        refresh();

        assertEquals(Optional.empty(), bossBarText(hunter));
    }

    @Test
    void switchingModesMidRoundRemovesTheOldDisplay() throws IOException {
        refresh();
        assertTrue(bossBarText(hunter).isPresent());

        fixture.reloadWith(hunter, ConfigKey.DISPLAY_MODE, "none");
        refresh();

        assertEquals(Optional.empty(), bossBarText(hunter));
    }

    @Test
    void modeNoneShowsNothing() throws IOException {
        fixture.reloadWith(hunter, ConfigKey.DISPLAY_MODE, "none");
        refresh();

        assertEquals(Optional.empty(), bossBarText(hunter));
    }
}
