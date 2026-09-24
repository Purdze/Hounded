package dev.marshall.hounded.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.tracking.Dimension;
import dev.marshall.hounded.tracking.TrackingReading;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HudComposerTest {
    private static final HudLine TIMER_AT_ONE_MINUTE =
            HudLine.of(MessageKey.DISPLAY_TIMER, Map.of(PlaceholderNames.TIME, "1:00"));

    private static HudInput running(Role role, Optional<TrackingReading> reading, boolean showDistance) {
        return new HudInput(
                GameState.RUNNING,
                Duration.ZERO,
                Duration.ofSeconds(30),
                Duration.ofMinutes(1),
                Optional.of(role),
                reading.map(known -> new HudInput.Tracking(known, "Steve")),
                showDistance);
    }

    private static TrackingReading reading(
            TrackingReading.Kind kind, Optional<Dimension> runnerDimension, OptionalInt distance) {
        return new TrackingReading(UUID.randomUUID(), kind, runnerDimension, distance);
    }

    private static List<HudLine> hunterLines(TrackingReading reading) {
        return HudComposer.compose(running(Role.HUNTER, Optional.of(reading), true))
                .lines();
    }

    @Test
    void headstartCountsDownForEveryoneInTheRound() {
        for (Role role : Role.values()) {
            HudFrame frame = HudComposer.compose(new HudInput(
                    GameState.HEADSTART,
                    Duration.ofSeconds(15),
                    Duration.ofSeconds(60),
                    Duration.ZERO,
                    Optional.of(role),
                    Optional.empty(),
                    true));

            assertEquals(
                    List.of(HudLine.of(MessageKey.DISPLAY_HEADSTART, Map.of(PlaceholderNames.TIME, "0:15"))),
                    frame.lines());
            assertEquals(0.25f, frame.progress());
        }
    }

    @Test
    void runnersOnlySeeTheTimer() {
        HudFrame frame = HudComposer.compose(running(Role.RUNNER, Optional.empty(), true));

        assertEquals(List.of(TIMER_AT_ONE_MINUTE), frame.lines());
        assertEquals(1f, frame.progress());
    }

    @Test
    void huntersSeeTheDistanceToTheirRunner() {
        assertEquals(
                List.of(
                        TIMER_AT_ONE_MINUTE,
                        HudLine.of(
                                MessageKey.DISPLAY_DISTANCE,
                                Map.of(PlaceholderNames.RUNNER, "Steve", PlaceholderNames.DISTANCE, "87"))),
                hunterLines(
                        reading(TrackingReading.Kind.RUNNER, Optional.of(Dimension.OVERWORLD), OptionalInt.of(87))));
    }

    @Test
    void huntersSeeWhichDimensionTheRunnerIsInAndHowFarThePortalIs() {
        assertEquals(
                new HudLine(
                        MessageKey.DISPLAY_DISTANCE_PORTAL,
                        Map.of(PlaceholderNames.RUNNER, "Steve", PlaceholderNames.DISTANCE, "45"),
                        Map.of(PlaceholderNames.DIMENSION, MessageKey.DIMENSION_NETHER)),
                hunterLines(reading(TrackingReading.Kind.PORTAL, Optional.of(Dimension.NETHER), OptionalInt.of(45)))
                        .getLast());
    }

    @Test
    void huntersAreToldWhenThereIsNoTrailOrTrackingIsOff() {
        assertEquals(
                HudLine.of(MessageKey.DISPLAY_NO_TRAIL, Map.of(PlaceholderNames.RUNNER, "Steve")),
                hunterLines(reading(TrackingReading.Kind.NO_DATA, Optional.empty(), OptionalInt.empty()))
                        .getLast());
        assertEquals(
                HudLine.of(MessageKey.DISPLAY_TRACKING_OFF, Map.of()),
                hunterLines(reading(TrackingReading.Kind.DISABLED, Optional.empty(), OptionalInt.empty()))
                        .getLast());
    }

    @Test
    void hunterWithoutAReadingSeesTheTimerOnly() {
        assertEquals(
                List.of(TIMER_AT_ONE_MINUTE),
                HudComposer.compose(running(Role.HUNTER, Optional.empty(), true))
                        .lines());
    }

    @Test
    void distanceCanBeTurnedOff() {
        TrackingReading reading =
                reading(TrackingReading.Kind.RUNNER, Optional.of(Dimension.OVERWORLD), OptionalInt.of(87));

        HudFrame frame = HudComposer.compose(running(Role.HUNTER, Optional.of(reading), false));

        assertEquals(List.of(TIMER_AT_ONE_MINUTE), frame.lines());
    }

    @Test
    void nothingIsShownOutsideARoundOrToPlayersNotInIt() {
        assertTrue(isHidden(GameState.LOBBY, Optional.of(Role.HUNTER)));
        assertTrue(isHidden(GameState.ENDED, Optional.of(Role.HUNTER)));
        assertTrue(isHidden(GameState.RUNNING, Optional.empty()));
    }

    private static boolean isHidden(GameState state, Optional<Role> role) {
        return HudComposer.compose(
                        new HudInput(state, Duration.ZERO, Duration.ZERO, Duration.ZERO, role, Optional.empty(), true))
                .isHidden();
    }
}
