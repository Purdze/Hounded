package dev.marshall.hounded.tracking;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * What a hunter's compass currently shows, for the compass itself and for the display.
 *
 * @param runnerDimension where the tracked runner is, if known
 * @param distance horizontal blocks to where the compass points; empty when it points nowhere useful
 */
public record TrackingReading(UUID runner, Kind kind, Optional<Dimension> runnerDimension, OptionalInt distance) {

    public enum Kind {
        /** Same dimension: pointing at the runner. */
        RUNNER,
        /** Different dimension: pointing at the runner's exit portal. */
        PORTAL,
        /** Nothing known in the hunter's dimension: pointing at spawn. */
        NO_DATA,
        /** Tracking is switched off in the hunter's dimension. */
        DISABLED
    }

    public TrackingReading {
        Objects.requireNonNull(runner, "runner");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(runnerDimension, "runnerDimension");
        Objects.requireNonNull(distance, "distance");
    }
}
