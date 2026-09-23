package dev.marshall.hounded.round;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Builds its own {@link RoundService}, because the plugin's session runs on the real clock. */
class RoundServiceTest {
    private static final long ONE_SECOND_OF_TICKS = 20;

    private final MutableClock clock = new MutableClock();
    private final GameSession session = new GameSession(clock);
    private final UUID runner = UUID.randomUUID();
    private PluginFixture fixture;
    private RoundService roundService;
    private PlayerMock watcher;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        roundService = new RoundService(fixture.plugin(), session, fixture.config());
        watcher = fixture.server().addPlayer("Watcher");
        session.assignRole(runner, Role.RUNNER);
        session.assignRole(UUID.randomUUID(), Role.HUNTER);
    }

    @AfterEach
    void tearDown() {
        roundService.shutdown();
        fixture.close();
    }

    private void passTime(Duration duration) {
        clock.advance(duration);
        fixture.server().getScheduler().performTicks(ONE_SECOND_OF_TICKS);
    }

    @Test
    void headstartIsAnnouncedAndHuntersAreReleasedWhenItEnds() {
        roundService.start(10);
        assertEquals(
                List.of(fixture.chat(MessageKey.START_HEADSTART, Placeholder.unparsed(PlaceholderNames.SECONDS, "10"))),
                messagesOf(watcher));

        passTime(Duration.ofSeconds(9));
        assertEquals(List.of(), messagesOf(watcher));

        passTime(Duration.ofSeconds(1));
        assertEquals(List.of(fixture.chat(MessageKey.START_RELEASED)), messagesOf(watcher));
        assertEquals(GameState.RUNNING, session.state());
        assertFalse(fixture.hasScheduledTasks());
    }

    @Test
    void lastRunnerDyingAnnouncesHuntersWinWithTheHuntTime() {
        roundService.start(0);
        passTime(Duration.ofSeconds(65));
        messagesOf(watcher);

        roundService.recordRunnerDeath(runner);

        assertEquals(List.of(fixture.winMessage(MessageKey.WIN_HUNTERS, "1:05")), messagesOf(watcher));
    }

    @Test
    void dragonDyingAnnouncesRunnersWin() {
        roundService.start(0);
        passTime(Duration.ofMinutes(42));
        messagesOf(watcher);

        roundService.recordDragonKilled();

        assertEquals(List.of(fixture.winMessage(MessageKey.WIN_RUNNERS, "42:00")), messagesOf(watcher));
    }

    @Test
    void endedRoundReturnsToLobbyWithRolesKept() {
        roundService.start(0);
        roundService.recordDragonKilled();

        assertEquals(GameState.LOBBY, session.state());
        assertEquals(List.of(runner), session.playersWith(Role.RUNNER));
    }

    @Test
    void stoppingDuringHeadstartCancelsTheTimer() {
        roundService.start(30);
        assertTrue(fixture.hasScheduledTasks());
        messagesOf(watcher);

        roundService.stop();

        assertEquals(List.of(fixture.chat(MessageKey.STOP_STOPPED)), messagesOf(watcher));
        assertFalse(fixture.hasScheduledTasks());
        assertEquals(GameState.LOBBY, session.state());
    }
}
