package dev.marshall.hounded.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TargetResolverTest {
    private final TargetResolver resolver = new TargetResolver();

    private static final Position RUNNER_IN_NETHER = new Position(10.5, 64, -20.5);
    private static final Position RUNNER_IN_OVERWORLD = new Position(100, 70, 200);
    private static final Position OVERWORLD_PORTAL = new Position(80, 68, 190);

    private static Optional<DimensionalPosition> runnerAt(Dimension dimension, Position position) {
        return Optional.of(new DimensionalPosition(dimension, position));
    }

    @Test
    void sameDimensionPointsAtRunner() {
        var snapshot = new TrackingSnapshot(
                Dimension.OVERWORLD,
                runnerAt(Dimension.OVERWORLD, RUNNER_IN_OVERWORLD),
                Map.of(Dimension.OVERWORLD, OVERWORLD_PORTAL));

        assertEquals(new CompassTarget.Runner(RUNNER_IN_OVERWORLD), resolver.resolve(snapshot));
    }

    @Test
    void differentDimensionPointsAtLastPortalInHuntersDimension() {
        var snapshot = new TrackingSnapshot(
                Dimension.OVERWORLD,
                runnerAt(Dimension.NETHER, RUNNER_IN_NETHER),
                Map.of(Dimension.OVERWORLD, OVERWORLD_PORTAL));

        assertEquals(new CompassTarget.LastPortal(OVERWORLD_PORTAL), resolver.resolve(snapshot));
    }

    @Test
    void differentDimensionWithoutAnyPortalIsNoData() {
        var snapshot =
                new TrackingSnapshot(Dimension.OVERWORLD, runnerAt(Dimension.NETHER, RUNNER_IN_NETHER), Map.of());

        assertEquals(new CompassTarget.NoData(Dimension.OVERWORLD), resolver.resolve(snapshot));
    }

    @Test
    void portalKnownOnlyInAnotherDimensionIsNoData() {
        var snapshot = new TrackingSnapshot(
                Dimension.END,
                runnerAt(Dimension.NETHER, RUNNER_IN_NETHER),
                Map.of(Dimension.OVERWORLD, OVERWORLD_PORTAL));

        assertEquals(new CompassTarget.NoData(Dimension.END), resolver.resolve(snapshot));
    }

    @Test
    void unknownRunnerLocationIsNoData() {
        var snapshot =
                new TrackingSnapshot(Dimension.NETHER, Optional.empty(), Map.of(Dimension.NETHER, RUNNER_IN_NETHER));

        assertEquals(new CompassTarget.NoData(Dimension.NETHER), resolver.resolve(snapshot));
    }
}
