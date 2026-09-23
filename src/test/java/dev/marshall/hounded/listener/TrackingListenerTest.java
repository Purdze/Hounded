package dev.marshall.hounded.listener;

import static dev.marshall.hounded.testing.PluginFixture.compassTarget;
import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Real teleports and the real command reach the plugin's tracking. */
class TrackingListenerTest {
    private PluginFixture fixture;
    private World overworld;
    private World nether;
    private PlayerMock hunter;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        overworld = fixture.addWorld("world", World.Environment.NORMAL);
        nether = fixture.addWorld("world_nether", World.Environment.NETHER);
        hunter = fixture.addAdmin("Hunter");
        runner = fixture.server().addPlayer("Runner");
        fixture.startRound(hunter, 0, runner);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void compassCommandGivesAHunterACompassWithInstructions() {
        runner.teleport(new Location(overworld, 40, 64, 40));

        hunter.performCommand("hounded compass");

        assertEquals(Optional.of(new Location(overworld, 40, 64, 40)), compassTarget(hunter));
        assertTrue(messagesOf(hunter).stream().anyMatch(message -> message.contains(runner.getName())));
    }

    @Test
    void compassCommandRefusesPlayersWhoAreNotHunting() {
        runner.performCommand("hounded compass");

        assertEquals(
                fixture.chat(MessageKey.COMPASS_NOT_IN_ROUND),
                messagesOf(runner).getLast());
    }

    @Test
    void portalTripIsRememberedForHuntersLeftBehind() {
        runner.teleport(new Location(overworld, 10, 70, 20));
        runner.teleport(new Location(nether, 1, 64, 2));

        hunter.performCommand("hounded compass");

        assertEquals(Optional.of(new Location(overworld, 10, 70, 20)), compassTarget(hunter));
    }
}
