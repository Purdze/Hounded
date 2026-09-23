package dev.marshall.hounded.listener;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.List;
import org.bukkit.entity.EnderDragon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Real death events on the mock server reach the plugin's round. */
class RoundListenerTest {
    // The round lasts milliseconds here, and the timer shows whole seconds.
    private static final String INSTANT = "0:00";

    private PluginFixture fixture;
    private PlayerMock admin;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        admin = fixture.addAdmin("Admin");
        runner = fixture.server().addPlayer("Runner");
        fixture.startRound(admin, runner, 0);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void runnerDeathEndsTheRoundForHunters() {
        runner.setHealth(0);

        assertEquals(List.of(fixture.winMessage(MessageKey.WIN_HUNTERS, INSTANT)), messagesOf(admin));
    }

    @Test
    void hunterDeathDoesNotEndTheRound() {
        admin.setHealth(0);

        assertEquals(List.of(), messagesOf(admin));
    }

    @Test
    void dragonDeathEndsTheRoundForRunners() {
        EnderDragon dragon = runner.getWorld().spawn(runner.getLocation(), EnderDragon.class);

        dragon.setHealth(0);

        assertEquals(List.of(fixture.winMessage(MessageKey.WIN_RUNNERS, INSTANT)), messagesOf(admin));
    }
}
