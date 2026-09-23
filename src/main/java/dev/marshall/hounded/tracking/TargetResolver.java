package dev.marshall.hounded.tracking;

import java.util.Objects;
import java.util.Optional;

/** Decides where a hunter's compass points. Pure: same snapshot in, same target out. */
public final class TargetResolver {

    public CompassTarget resolve(TrackingSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Dimension hunterDimension = snapshot.hunterDimension();
        Optional<DimensionalPosition> runner = snapshot.runnerLocation();

        if (runner.isEmpty()) {
            return new CompassTarget.NoData(hunterDimension);
        }
        if (runner.get().dimension() == hunterDimension) {
            return new CompassTarget.Runner(runner.get().position());
        }
        Position portal = snapshot.lastPortalByDimension().get(hunterDimension);
        if (portal == null) {
            return new CompassTarget.NoData(hunterDimension);
        }
        return new CompassTarget.LastPortal(portal);
    }
}
