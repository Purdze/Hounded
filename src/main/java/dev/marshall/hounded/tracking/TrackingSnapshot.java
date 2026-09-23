package dev.marshall.hounded.tracking;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything {@link TargetResolver} needs to decide where one hunter's compass points.
 *
 * @param hunterDimension the dimension the hunter is in
 * @param runnerLocation the tracked runner's current location, empty if unknown (e.g. offline)
 * @param lastPortalByDimension for each dimension, where the runner last left it through a portal
 */
public record TrackingSnapshot(
        Dimension hunterDimension,
        Optional<DimensionalPosition> runnerLocation,
        Map<Dimension, Position> lastPortalByDimension) {

    public TrackingSnapshot {
        Objects.requireNonNull(hunterDimension, "hunterDimension");
        Objects.requireNonNull(runnerLocation, "runnerLocation");
        lastPortalByDimension = Map.copyOf(lastPortalByDimension);
    }
}
