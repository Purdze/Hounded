package dev.marshall.hounded.listener;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Real events on the mock server reach the plugin's round. */
class RoundListenerTest {
    // The round lasts milliseconds here, and the timer shows whole seconds.
    private static final String INSTANT = "0:00";

    private PluginFixture fixture;
    private PlayerMock admin;
    private PlayerMock runner;
    private PlayerMock otherRunner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        admin = fixture.addAdmin("Admin");
        runner = fixture.server().addPlayer("Runner");
        otherRunner = fixture.server().addPlayer("OtherRunner");
        fixture.startRound(admin, 0, runner, otherRunner);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void runnerDeathEliminatesThemAndTheLastOneEndsTheRound() {
        runner.setHealth(0);
        assertEquals(List.of(fixture.aboutPlayer(MessageKey.ROUND_RUNNER_ELIMINATED, runner)), messagesOf(admin));

        otherRunner.setHealth(0);
        assertEquals(List.of(fixture.winMessage(MessageKey.WIN_HUNTERS, INSTANT)), messagesOf(admin));
    }

    @Test
    void hunterDeathDoesNotAffectTheRound() {
        admin.setHealth(0);

        assertEquals(List.of(), messagesOf(admin));
    }

    @Test
    void dragonDeathEndsTheRoundForRunners() {
        EnderDragon dragon = runner.getWorld().spawn(runner.getLocation(), EnderDragon.class);

        dragon.setHealth(0);

        assertEquals(List.of(fixture.winMessage(MessageKey.WIN_RUNNERS, INSTANT)), messagesOf(admin));
    }

    @Test
    void runnerQuittingIsAnnounced() {
        runner.disconnect();

        assertEquals(List.of(fixture.runnerLeftMessage(runner)), messagesOf(admin));
    }

    @Test
    void eliminatedRunnerSpectatesAfterRespawning() {
        runner.setHealth(0);

        // MockBukkit's respawn() does not fire Paper's post-respawn event, so fire it directly.
        fixture.server()
                .getPluginManager()
                .callEvent(new PlayerPostRespawnEvent(
                        runner, runner.getLocation(), false, false, false, PlayerRespawnEvent.RespawnReason.DEATH));

        assertEquals(GameMode.SPECTATOR, runner.getGameMode());
    }
}
