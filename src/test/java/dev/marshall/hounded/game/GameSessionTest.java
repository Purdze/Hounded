package dev.marshall.hounded.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.testing.MutableClock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameSessionTest {
    private final MutableClock clock = new MutableClock();
    private final GameSession session = new GameSession(clock);
    private final UUID runner = UUID.randomUUID();
    private final UUID secondRunner = UUID.randomUUID();
    private final UUID hunter = UUID.randomUUID();

    private void assignOneRunnerAndHunter() {
        session.assignRole(runner, Role.RUNNER);
        session.assignRole(hunter, Role.HUNTER);
    }

    private static TransitionResult changed(GameState from, GameState to) {
        return new TransitionResult.Changed(from, to);
    }

    private static TransitionResult unchanged(GameState state) {
        return new TransitionResult.Unchanged(state);
    }

    private static TransitionResult rejected(RejectionReason reason) {
        return new TransitionResult.Rejected(reason);
    }

    @Test
    void startsInLobbyWithoutOutcome() {
        assertEquals(GameState.LOBBY, session.state());
        assertEquals(Optional.empty(), session.outcome());
    }

    @Nested
    class Starting {
        @Test
        void withHeadstartEntersHeadstart() {
            assignOneRunnerAndHunter();
            assertEquals(changed(GameState.LOBBY, GameState.HEADSTART), session.start(30));
            assertEquals(Duration.ofSeconds(30), session.headstartRemaining());
            assertEquals(Duration.ofSeconds(30), session.headstartLength());
        }

        @Test
        void withZeroHeadstartGoesStraightToRunning() {
            assignOneRunnerAndHunter();
            assertEquals(changed(GameState.LOBBY, GameState.RUNNING), session.start(0));
        }

        @Test
        void rejectsNegativeHeadstart() {
            assignOneRunnerAndHunter();
            assertEquals(rejected(RejectionReason.NEGATIVE_HEADSTART), session.start(-1));
            assertEquals(GameState.LOBBY, session.state());
        }

        @Test
        void rejectsWithoutRunners() {
            session.assignRole(hunter, Role.HUNTER);
            assertEquals(rejected(RejectionReason.NO_RUNNERS), session.start(0));
        }

        @Test
        void rejectsWithoutHunters() {
            session.assignRole(runner, Role.RUNNER);
            assertEquals(rejected(RejectionReason.NO_HUNTERS), session.start(0));
        }

        @Test
        void rejectsWhenAlreadyStarted() {
            assignOneRunnerAndHunter();
            session.start(0);
            assertEquals(rejected(RejectionReason.NOT_IN_LOBBY), session.start(0));
        }
    }

    @Nested
    class Checks {
        @Test
        void checkStartGivesTheReasonStartWouldBeRefused() {
            assertEquals(Optional.of(RejectionReason.NO_RUNNERS), session.checkStart(0));
            session.assignRole(runner, Role.RUNNER);
            assertEquals(Optional.of(RejectionReason.NO_HUNTERS), session.checkStart(0));
            session.assignRole(hunter, Role.HUNTER);
            assertEquals(Optional.of(RejectionReason.NEGATIVE_HEADSTART), session.checkStart(-1));
            assertEquals(Optional.empty(), session.checkStart(0));

            session.start(0);
            assertEquals(Optional.of(RejectionReason.NOT_IN_LOBBY), session.checkStart(0));
        }

        @Test
        void checkingDoesNotStartAnything() {
            assignOneRunnerAndHunter();
            session.checkStart(0);
            assertEquals(GameState.LOBBY, session.state());
        }

        @Test
        void rolesAreLockedOnlyWhileARoundIsActive() {
            assignOneRunnerAndHunter();
            assertFalse(session.rolesLocked());
            session.start(10);
            assertTrue(session.rolesLocked());
            session.stop();
            assertFalse(session.rolesLocked());
        }
    }

    @Nested
    class Headstart {
        @Test
        void tickBeforeHeadstartEndsKeepsHeadstart() {
            assignOneRunnerAndHunter();
            session.start(10);
            clock.advance(Duration.ofSeconds(9));
            assertEquals(unchanged(GameState.HEADSTART), session.tick());
            assertEquals(Duration.ofSeconds(1), session.headstartRemaining());
        }

        @Test
        void tickAfterHeadstartEndsReleasesHunters() {
            assignOneRunnerAndHunter();
            session.start(10);
            clock.advance(Duration.ofSeconds(10));
            assertEquals(changed(GameState.HEADSTART, GameState.RUNNING), session.tick());
            assertEquals(Duration.ZERO, session.headstartRemaining());
        }

        @Test
        void huntTimerStartsWhenHuntersAreReleased() {
            assignOneRunnerAndHunter();
            session.start(10);
            clock.advance(Duration.ofSeconds(10));
            assertEquals(Duration.ZERO, session.elapsedHuntTime());
            session.tick();
            clock.advance(Duration.ofSeconds(5));
            assertEquals(Duration.ofSeconds(5), session.elapsedHuntTime());
        }

        @Test
        void tickOutsideHeadstartChangesNothing() {
            assertEquals(unchanged(GameState.LOBBY), session.tick());
        }
    }

    @Test
    void playersArePlayingTheirRoleOnlyDuringARound() {
        assignOneRunnerAndHunter();
        assertFalse(session.isPlaying(hunter, Role.HUNTER));

        session.start(10);
        assertTrue(session.isPlaying(hunter, Role.HUNTER));
        assertFalse(session.isPlaying(hunter, Role.RUNNER));

        session.stop();
        assertFalse(session.isPlaying(hunter, Role.HUNTER));
    }

    @Nested
    class HeadstartHold {
        @Test
        void onlyHuntersAreHeldAndOnlyDuringTheHeadstart() {
            assignOneRunnerAndHunter();
            assertFalse(session.isHeldInHeadstart(hunter));

            session.start(10);
            assertTrue(session.isHeldInHeadstart(hunter));
            assertFalse(session.isHeldInHeadstart(runner));

            clock.advance(Duration.ofSeconds(10));
            session.tick();
            assertFalse(session.isHeldInHeadstart(hunter));

            session.stop();
            assertFalse(session.isHeldInHeadstart(hunter));
        }
    }

    @Nested
    class WinConditions {
        @Test
        void dragonKillMeansRunnersWin() {
            assignOneRunnerAndHunter();
            session.start(0);
            assertEquals(changed(GameState.RUNNING, GameState.ENDED), session.recordDragonKilled());
            assertEquals(Optional.of(GameOutcome.RUNNERS_WIN), session.outcome());
        }

        @Test
        void dragonKillDuringHeadstartStillCounts() {
            assignOneRunnerAndHunter();
            session.start(60);
            assertEquals(changed(GameState.HEADSTART, GameState.ENDED), session.recordDragonKilled());
            assertEquals(Optional.of(GameOutcome.RUNNERS_WIN), session.outcome());
        }

        @Test
        void onlyRunnerDyingMeansHuntersWin() {
            assignOneRunnerAndHunter();
            session.start(0);
            assertEquals(changed(GameState.RUNNING, GameState.ENDED), session.eliminateRunner(runner));
            assertEquals(Optional.of(GameOutcome.HUNTERS_WIN), session.outcome());
        }

        @Test
        void huntersWinOnlyAfterEveryRunnerDied() {
            assignOneRunnerAndHunter();
            session.assignRole(secondRunner, Role.RUNNER);
            session.start(0);

            assertEquals(unchanged(GameState.RUNNING), session.eliminateRunner(runner));
            assertTrue(session.isEliminated(runner));
            assertEquals(List.of(secondRunner), session.remainingRunners());

            assertEquals(changed(GameState.RUNNING, GameState.ENDED), session.eliminateRunner(secondRunner));
            assertEquals(Optional.of(GameOutcome.HUNTERS_WIN), session.outcome());
        }

        @Test
        void sameRunnerDyingTwiceIsRejected() {
            assignOneRunnerAndHunter();
            session.assignRole(secondRunner, Role.RUNNER);
            session.start(0);
            session.eliminateRunner(runner);
            assertEquals(rejected(RejectionReason.ALREADY_ELIMINATED), session.eliminateRunner(runner));
            assertEquals(GameState.RUNNING, session.state());
        }

        @Test
        void hunterDeathIsNotARunnerDeath() {
            assignOneRunnerAndHunter();
            session.start(0);
            assertEquals(rejected(RejectionReason.NOT_A_RUNNER), session.eliminateRunner(hunter));
        }

        @Test
        void deathsAndDragonOutsideARoundAreRejected() {
            assignOneRunnerAndHunter();
            assertEquals(rejected(RejectionReason.NOT_ACTIVE), session.eliminateRunner(runner));
            assertEquals(rejected(RejectionReason.NOT_ACTIVE), session.recordDragonKilled());
        }

        @Test
        void huntTimerFreezesWhenRoundEnds() {
            assignOneRunnerAndHunter();
            session.start(0);
            clock.advance(Duration.ofSeconds(42));
            session.recordDragonKilled();
            clock.advance(Duration.ofMinutes(5));
            assertEquals(Duration.ofSeconds(42), session.elapsedHuntTime());
        }
    }

    @Nested
    class RejoinWindow {
        private static final Duration GRACE = Duration.ofMinutes(5);

        @Test
        void runnerWhoReturnsInTimeStaysIn() {
            assignOneRunnerAndHunter();
            session.start(0);

            assertEquals(unchanged(GameState.RUNNING), session.recordRunnerLeft(runner, GRACE));
            clock.advance(GRACE.minusSeconds(1));
            assertEquals(unchanged(GameState.RUNNING), session.recordRunnerReturned(runner));

            clock.advance(GRACE);
            assertEquals(List.of(), session.runnersPastRejoinDeadline());
            assertEquals(List.of(runner), session.remainingRunners());
        }

        @Test
        void runnerWhoStaysAwayIsReportedOnceTheDeadlinePasses() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.recordRunnerLeft(runner, GRACE);

            clock.advance(GRACE.minusSeconds(1));
            assertEquals(List.of(), session.runnersPastRejoinDeadline());
            clock.advance(Duration.ofSeconds(1));
            assertEquals(List.of(runner), session.runnersPastRejoinDeadline());
        }

        @Test
        void zeroGraceExpiresImmediately() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.recordRunnerLeft(runner, Duration.ZERO);

            assertEquals(List.of(runner), session.runnersPastRejoinDeadline());
        }

        @Test
        void awayRunnerStillCountsAsRemaining() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.recordRunnerLeft(runner, GRACE);

            assertEquals(List.of(runner), session.remainingRunners());
            assertEquals(GameState.RUNNING, session.state());
        }

        @Test
        void lastRunnerTimingOutMeansHuntersWin() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.recordRunnerLeft(runner, Duration.ZERO);

            assertEquals(changed(GameState.RUNNING, GameState.ENDED), session.eliminateRunner(runner));
            assertEquals(Optional.of(GameOutcome.HUNTERS_WIN), session.outcome());
            assertEquals(List.of(), session.runnersPastRejoinDeadline());
        }

        @Test
        void onlyActiveRunnersStillInTheRoundCanLeave() {
            assignOneRunnerAndHunter();
            session.assignRole(secondRunner, Role.RUNNER);
            assertEquals(rejected(RejectionReason.NOT_ACTIVE), session.recordRunnerLeft(runner, GRACE));

            session.start(0);
            session.eliminateRunner(runner);
            assertEquals(rejected(RejectionReason.ALREADY_ELIMINATED), session.recordRunnerLeft(runner, GRACE));
            assertEquals(rejected(RejectionReason.NOT_A_RUNNER), session.recordRunnerLeft(hunter, GRACE));
        }

        @Test
        void returningWithoutHavingLeftIsRejected() {
            assignOneRunnerAndHunter();
            session.start(0);

            assertEquals(rejected(RejectionReason.NOT_AWAITING_RETURN), session.recordRunnerReturned(runner));
        }

        @Test
        void resetForgetsRunnersWhoLeft() {
            assignOneRunnerAndHunter();
            session.assignRole(secondRunner, Role.RUNNER);
            session.start(0);
            session.recordRunnerLeft(runner, Duration.ZERO);
            session.stop();
            session.reset();

            assertEquals(List.of(), session.runnersPastRejoinDeadline());
        }
    }

    @Nested
    class StoppingAndReset {
        @Test
        void stopEndsRunningRound() {
            assignOneRunnerAndHunter();
            session.start(0);
            assertEquals(changed(GameState.RUNNING, GameState.ENDED), session.stop());
            assertEquals(Optional.of(GameOutcome.STOPPED), session.outcome());
        }

        @Test
        void stopEndsHeadstart() {
            assignOneRunnerAndHunter();
            session.start(30);
            assertEquals(changed(GameState.HEADSTART, GameState.ENDED), session.stop());
        }

        @Test
        void stopInLobbyIsRejected() {
            assertEquals(rejected(RejectionReason.NOT_ACTIVE), session.stop());
        }

        @Test
        void resetReturnsToLobbyAndKeepsRoles() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.eliminateRunner(runner);

            assertEquals(changed(GameState.ENDED, GameState.LOBBY), session.reset());
            assertEquals(Optional.empty(), session.outcome());
            assertFalse(session.isEliminated(runner));
            assertEquals(Duration.ZERO, session.elapsedHuntTime());
            assertEquals(Optional.of(Role.RUNNER), session.roleOf(runner));
        }

        @Test
        void resetOnlyWorksAfterRoundEnded() {
            assertEquals(rejected(RejectionReason.NOT_ENDED), session.reset());
            assignOneRunnerAndHunter();
            session.start(0);
            assertEquals(rejected(RejectionReason.NOT_ENDED), session.reset());
        }

        @Test
        void aNewRoundCanStartAfterReset() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.stop();
            session.reset();
            assertEquals(changed(GameState.LOBBY, GameState.RUNNING), session.start(0));
        }
    }

    @Nested
    class RoleLocking {
        @Test
        void rolesCannotChangeDuringARound() {
            assignOneRunnerAndHunter();
            session.start(30);
            UUID newcomer = UUID.randomUUID();
            assertEquals(rejected(RejectionReason.ROLES_LOCKED), session.assignRole(newcomer, Role.HUNTER));
            assertEquals(rejected(RejectionReason.ROLES_LOCKED), session.unassignRole(runner, Role.RUNNER));
            assertEquals(rejected(RejectionReason.ROLES_LOCKED), session.unassignRole(hunter, Role.RUNNER));
            assertEquals(Optional.of(Role.RUNNER), session.roleOf(runner));
        }

        @Test
        void removingARoleThePlayerDoesNotHoldIsRejected() {
            session.assignRole(hunter, Role.HUNTER);
            assertEquals(rejected(RejectionReason.NOT_IN_ROLE), session.unassignRole(hunter, Role.RUNNER));
            assertEquals(rejected(RejectionReason.NOT_IN_ROLE), session.unassignRole(runner, Role.RUNNER));
            assertEquals(Optional.of(Role.HUNTER), session.roleOf(hunter));
        }

        @Test
        void removingTheHeldRoleUnassignsThePlayer() {
            session.assignRole(hunter, Role.HUNTER);
            assertEquals(unchanged(GameState.LOBBY), session.unassignRole(hunter, Role.HUNTER));
            assertEquals(Optional.empty(), session.roleOf(hunter));
        }

        @Test
        void rolesCanChangeAfterRoundEnded() {
            assignOneRunnerAndHunter();
            session.start(0);
            session.stop();
            assertEquals(unchanged(GameState.ENDED), session.unassignRole(runner, Role.RUNNER));
            assertEquals(Optional.empty(), session.roleOf(runner));
        }
    }
}
