package dev.marshall.hounded.api;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static dev.marshall.hounded.testing.PluginFixture.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.EnderDragon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** The public events as another plugin sees them, driven by the real commands. */
class ApiEventsTest {
    private PluginFixture fixture;
    private PlayerMock admin;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        admin = fixture.addAdmin("Admin");
        runner = fixture.server().addPlayer("Runner");
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private boolean carriesCompass(PlayerMock player) {
        return player.getInventory().contains(Material.COMPASS);
    }

    @Test
    void startEventDescribesTheRound() {
        List<HoundedRoundStartEvent> starts = fixture.captureEvents(HoundedRoundStartEvent.class);

        fixture.startRound(admin, 15, runner);

        HoundedRoundStartEvent start = starts.getFirst();
        assertEquals(List.of(runner.getUniqueId()), start.runners());
        assertEquals(List.of(admin.getUniqueId()), start.hunters());
        assertEquals(15, start.headstartSeconds());
    }

    @Test
    void anotherPluginCanCancelTheStart() {
        fixture.cancelEvents(HoundedRoundStartEvent.class);
        fixture.assignRoles(admin, runner);
        messagesOf(admin);

        admin.performCommand("hounded start 0");

        assertEquals(List.of(fixture.chat(MessageKey.ERROR_CANCELLED_BY_PLUGIN)), messagesOf(admin));
        assertFalse(carriesCompass(admin));
        assertEquals(List.of(fixture.chat(MessageKey.STOP_NOT_RUNNING)), run(admin, "hounded stop"));
    }

    @Test
    void stoppingFiresAStopEventThatAnyEndListenerAlsoHears() {
        List<HoundedRoundStopEvent> stops = fixture.captureEvents(HoundedRoundStopEvent.class);
        List<HoundedRoundEndEvent> ends = fixture.captureEvents(HoundedRoundEndEvent.class);
        fixture.startRound(admin, 0, runner);

        admin.performCommand("hounded stop");

        assertEquals(1, stops.size());
        assertEquals(stops, ends);
        assertEquals(List.of(runner.getUniqueId()), stops.getFirst().runners());
    }

    @Test
    void dragonKillFiresARunnersWin() {
        List<HoundedRoundWinEvent> wins = fixture.captureEvents(HoundedRoundWinEvent.class);
        fixture.startRound(admin, 0, runner);

        runner.getWorld().spawn(runner.getLocation(), EnderDragon.class).setHealth(0);

        assertEquals(Role.RUNNER, wins.getFirst().winner());
    }

    @Test
    void lastRunnerDyingFiresAHuntersWin() {
        List<HoundedRoundEndEvent> ends = fixture.captureEvents(HoundedRoundEndEvent.class);
        fixture.startRound(admin, 0, runner);

        runner.setHealth(0);

        HoundedRoundWinEvent win = assertInstanceOf(HoundedRoundWinEvent.class, ends.getFirst());
        assertEquals(Role.HUNTER, win.winner());
        assertTrue(win.huntTime().toSeconds() < 5);
    }

    @Test
    void anotherPluginCanCancelARoleChange() {
        fixture.cancelEvents(HoundedRoleChangeEvent.class);
        messagesOf(admin);

        assertEquals(
                List.of(fixture.chat(MessageKey.ERROR_CANCELLED_BY_PLUGIN)), run(admin, "hounded runner add Runner"));
        assertFalse(String.join("", run(admin, "hounded runner list")).contains(runner.getName()));
    }
}
