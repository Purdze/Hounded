package dev.marshall.hounded.display;

import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.round.HuntTimeFormatter;
import dev.marshall.hounded.tracking.Dimension;
import dev.marshall.hounded.tracking.TrackingReading;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decides what each player's display shows. Pure, so every case is unit-tested: players in the
 * round see the headstart countdown or hunt timer, and hunters also see where their compass leads.
 */
public final class HudComposer {

    private HudComposer() {}

    public static HudFrame compose(HudInput input) {
        if (input.role().isEmpty()) {
            return HudFrame.HIDDEN;
        }
        return switch (input.state()) {
            case HEADSTART ->
                new HudFrame(
                        List.of(timeLine(MessageKey.DISPLAY_HEADSTART, input.headstartRemaining())),
                        fractionLeft(input.headstartRemaining(), input.headstartLength()));
            case RUNNING -> new HudFrame(runningLines(input), 1f);
            case LOBBY, ENDED -> HudFrame.HIDDEN;
        };
    }

    private static List<HudLine> runningLines(HudInput input) {
        HudLine timer = timeLine(MessageKey.DISPLAY_TIMER, input.elapsedHuntTime());
        if (!input.showDistance() || input.role().get() != Role.HUNTER) {
            return List.of(timer);
        }
        return input.tracking()
                .map(tracking -> List.of(timer, distanceLine(tracking)))
                .orElse(List.of(timer));
    }

    private static HudLine distanceLine(HudInput.Tracking tracking) {
        TrackingReading reading = tracking.reading();
        String runner = tracking.runnerName();
        Map<String, String> runnerAndDistance = Map.of(
                PlaceholderNames.RUNNER,
                runner,
                PlaceholderNames.DISTANCE,
                Integer.toString(reading.distance().orElse(0)));
        HudLine direct = HudLine.of(MessageKey.DISPLAY_DISTANCE, runnerAndDistance);
        return switch (reading.kind()) {
            case RUNNER -> direct;
            case PORTAL ->
                dimensionName(reading.runnerDimension())
                        .map(name -> new HudLine(
                                MessageKey.DISPLAY_DISTANCE_PORTAL,
                                runnerAndDistance,
                                Map.of(PlaceholderNames.DIMENSION, name)))
                        .orElse(direct);
            case NO_DATA -> HudLine.of(MessageKey.DISPLAY_NO_TRAIL, Map.of(PlaceholderNames.RUNNER, runner));
            case DISABLED -> HudLine.of(MessageKey.DISPLAY_TRACKING_OFF, Map.of());
        };
    }

    private static Optional<MessageKey> dimensionName(Optional<Dimension> dimension) {
        return dimension.map(known -> switch (known) {
            case OVERWORLD -> MessageKey.DIMENSION_OVERWORLD;
            case NETHER -> MessageKey.DIMENSION_NETHER;
            case END -> MessageKey.DIMENSION_END;
        });
    }

    private static HudLine timeLine(MessageKey key, Duration time) {
        return HudLine.of(key, Map.of(PlaceholderNames.TIME, HuntTimeFormatter.format(time)));
    }

    private static float fractionLeft(Duration remaining, Duration total) {
        if (total.isZero()) {
            return 0f;
        }
        return Math.clamp((float) remaining.toMillis() / total.toMillis(), 0f, 1f);
    }
}
