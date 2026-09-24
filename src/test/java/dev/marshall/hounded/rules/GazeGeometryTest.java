package dev.marshall.hounded.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GazeGeometryTest {
    private static final Vector3 EYE = new Vector3(0, 65, 0);
    private static final Vector3 EAST = new Vector3(1, 0, 0);
    private static final double MAX_ANGLE = 15;
    private static final double MAX_DISTANCE = 64;

    /** A target {@code distance} blocks away, {@code degrees} to the side of looking east. */
    private static boolean seesTargetAt(double degrees, double distance) {
        double radians = Math.toRadians(degrees);
        Vector3 target = new Vector3(distance * Math.cos(radians), 65, distance * Math.sin(radians));
        return GazeGeometry.isLookingAt(EYE, EAST, target, MAX_ANGLE, MAX_DISTANCE);
    }

    @Test
    void targetStraightAheadIsSeen() {
        assertTrue(seesTargetAt(0, 10));
    }

    @Test
    void targetNearTheCrosshairIsSeenButFurtherOffIsNot() {
        assertTrue(seesTargetAt(10, 10));
        assertFalse(seesTargetAt(20, 10));
    }

    @Test
    void targetBehindIsNotSeen() {
        assertFalse(seesTargetAt(180, 10));
    }

    @Test
    void targetTooFarAwayIsNotSeen() {
        assertTrue(seesTargetAt(0, MAX_DISTANCE));
        assertFalse(seesTargetAt(0, MAX_DISTANCE + 1));
    }

    @Test
    void directionDoesNotNeedToBeNormalised() {
        assertTrue(
                GazeGeometry.isLookingAt(EYE, new Vector3(5, 0, 0), new Vector3(10, 65, 0), MAX_ANGLE, MAX_DISTANCE));
    }
}
