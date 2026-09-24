package dev.marshall.hounded.round;

import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Builds its own {@link RoundService}, because the plugin's session runs on the real clock. */
class RoundServiceTest {
    private static final long ONE_SECOND_OF_TICKS = 20;

    private final MutableClock clock = new MutableClock();
    private final GameSession session = new GameSession(clock);
    private PluginFixture fixture;
    private RoundService roundService;
    private PlayerMock watcher;
    private PlayerMock runner;
    private PlayerMock hunter;
    private int tasksBeforeTheRound;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        roundService = fixture.roundServiceFor(session);
        tasksBeforeTheRound = fixture.scheduledTaskCount();
        watcher = fixture.server().addPlayer("Watcher");
        runner = fixture.server().addPlayer("Runner");
        session.assignRole(runner.getUniqueId(), Role.RUNNER);
        hunter = fixture.server().addPlayer("Hunter");
        session.assignRole(hunter.getUniqueId(), Role.HUNTER);
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

    private PlayerMock addSecondRunner() {
        PlayerMock second = fixture.server().addPlayer("SecondRunner");
        session.assignRole(second.getUniqueId(), Role.RUNNER);
        return second;
    }

    private boolean roundTimerRunning() {
        return fixture.scheduledTaskCount() > tasksBeforeTheRound;
    }

    private int graceSeconds() {
        return fixture.config().settings().rules().runnerRejoinGraceSeconds();
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
    }

    @Test
    void lastRunnerDyingAnnouncesHuntersWinWithTheHuntTime() {
        roundService.start(0);
        passTime(Duration.ofSeconds(65));
        messagesOf(watcher);

        roundService.recordRunnerDeath(runner.getUniqueId());

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
    void endedRoundReturnsToLobbyWithRolesKeptAndNoTimer() {
        roundService.start(0);
        assertTrue(roundTimerRunning());

        roundService.recordDragonKilled();

        assertEquals(GameState.LOBBY, session.state());
        assertEquals(List.of(runner.getUniqueId()), session.playersWith(Role.RUNNER));
        assertFalse(roundTimerRunning());
    }

    @Test
    void stoppingDuringHeadstartCancelsTheTimer() {
        roundService.start(30);
        messagesOf(watcher);

        roundService.stop();

        assertEquals(List.of(fixture.chat(MessageKey.STOP_STOPPED)), messagesOf(watcher));
        assertFalse(roundTimerRunning());
        assertEquals(GameState.LOBBY, session.state());
    }

    @Test
    void runnerWhoLeavesAndStaysAwayTimesOut() {
        roundService.start(0);
        messagesOf(watcher);

        roundService.playerLeft(runner.getUniqueId());
        assertEquals(List.of(fixture.runnerLeftMessage(runner)), messagesOf(watcher));

        passTime(Duration.ofSeconds(graceSeconds()));

        assertEquals(
                List.of(
                        fixture.aboutPlayer(MessageKey.ROUND_RUNNER_TIMED_OUT, runner),
                        fixture.winMessage(MessageKey.WIN_HUNTERS, "5:00")),
                messagesOf(watcher));
        assertEquals(GameState.LOBBY, session.state());
    }

    @Test
    void runnerWhoComesBackInTimeStaysIn() {
        roundService.start(0);
        roundService.playerLeft(runner.getUniqueId());
        passTime(Duration.ofSeconds(10));
        messagesOf(watcher);

        roundService.playerJoined(runner);
        assertEquals(List.of(fixture.aboutPlayer(MessageKey.ROUND_RUNNER_RETURNED, runner)), messagesOf(watcher));

        passTime(Duration.ofSeconds(graceSeconds()));
        assertEquals(List.of(), messagesOf(watcher));
        assertEquals(GameState.RUNNING, session.state());
    }

    @Test
    void withNoGraceALeavingRunnerIsOutOnTheNextTick() throws IOException {
        fixture.setConfig(ConfigKey.RULES_RUNNER_REJOIN_GRACE_SECONDS, 0);
        addSecondRunner();
        roundService.start(0);
        messagesOf(watcher);

        roundService.playerLeft(runner.getUniqueId());
        assertEquals(List.of(), messagesOf(watcher));

        passTime(Duration.ZERO);
        assertEquals(List.of(fixture.aboutPlayer(MessageKey.ROUND_RUNNER_TIMED_OUT, runner)), messagesOf(watcher));
        assertTrue(session.isEliminated(runner.getUniqueId()));
    }

    @Test
    void eliminatedRunnerSpectatesAndGetsTheirModeBackWhenTheRoundEnds() {
        addSecondRunner();
        runner.setGameMode(GameMode.ADVENTURE);
        roundService.start(0);
        messagesOf(watcher);

        roundService.recordRunnerDeath(runner.getUniqueId());
        assertEquals(List.of(fixture.aboutPlayer(MessageKey.ROUND_RUNNER_ELIMINATED, runner)), messagesOf(watcher));
        roundService.playerRespawned(runner);
        assertEquals(GameMode.SPECTATOR, runner.getGameMode());

        roundService.stop();
        assertEquals(GameMode.ADVENTURE, runner.getGameMode());
    }

    @Test
    void eliminatedRunnerKeepsPlayingWhenSpectatingIsOff() throws IOException {
        fixture.setConfig(ConfigKey.RULES_ELIMINATED_RUNNERS_SPECTATE, false);
        addSecondRunner();
        roundService.start(0);

        roundService.recordRunnerDeath(runner.getUniqueId());
        roundService.playerRespawned(runner);

        assertEquals(GameMode.SURVIVAL, runner.getGameMode());
    }

    @Test
    void spectatorWhoIsOfflineWhenTheRoundEndsIsRestoredOnJoin() {
        addSecondRunner();
        roundService.start(0);
        roundService.recordRunnerDeath(runner.getUniqueId());
        roundService.playerRespawned(runner);
        runner.disconnect();

        roundService.stop();
        assertEquals(GameMode.SPECTATOR, runner.getGameMode());

        runner.reconnect();
        roundService.playerJoined(runner);
        assertEquals(GameMode.SURVIVAL, runner.getGameMode());
    }

    @Test
    void runnerWhoTimedOutSpectatesWhenTheyRejoin() throws IOException {
        fixture.setConfig(ConfigKey.RULES_RUNNER_REJOIN_GRACE_SECONDS, 0);
        addSecondRunner();
        roundService.start(0);
        roundService.playerLeft(runner.getUniqueId());
        passTime(Duration.ZERO);
        messagesOf(watcher);

        roundService.playerJoined(runner);

        assertEquals(GameMode.SPECTATOR, runner.getGameMode());
        assertEquals(List.of(), messagesOf(watcher));
    }

    @Test
    void huntersAreToldTheyAreFrozenAndBlindedDuringTheHeadstart() {
        roundService.start(10);

        assertTrue(messagesOf(hunter).contains(fixture.chat(MessageKey.START_FROZEN)));
        assertTrue(hunter.hasPotionEffect(PotionEffectType.BLINDNESS));
        assertFalse(runner.hasPotionEffect(PotionEffectType.BLINDNESS));
    }

    @Test
    void blindnessIsLiftedWhenHuntersAreReleased() {
        roundService.start(10);

        passTime(Duration.ofSeconds(10));

        assertFalse(hunter.hasPotionEffect(PotionEffectType.BLINDNESS));
    }

    @Test
    void blindnessIsLiftedWhenTheRoundIsStopped() {
        roundService.start(10);

        roundService.stop();

        assertFalse(hunter.hasPotionEffect(PotionEffectType.BLINDNESS));
    }

    @Test
    void hunterJoiningDuringTheHeadstartIsHeldToo() {
        hunter.disconnect();
        roundService.start(10);

        hunter.reconnect();
        roundService.playerJoined(hunter);

        assertTrue(hunter.hasPotionEffect(PotionEffectType.BLINDNESS));
    }

    @Test
    void noBlindnessWhenTurnedOff() throws IOException {
        fixture.setConfig(ConfigKey.HEADSTART_BLIND_HUNTERS, false);

        roundService.start(10);

        assertFalse(hunter.hasPotionEffect(PotionEffectType.BLINDNESS));
    }
}
