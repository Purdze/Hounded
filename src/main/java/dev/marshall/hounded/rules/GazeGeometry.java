package dev.marshall.hounded.rules;

/** Whether someone looking in a direction has a target near their crosshair. Line of sight is checked separately. */
public final class GazeGeometry {

    private GazeGeometry() {}

    /**
     * @param eye where the looker's eyes are
     * @param direction where they are looking; need not be normalised
     * @param target the point being looked at
     */
    public static boolean isLookingAt(
            Vector3 eye, Vector3 direction, Vector3 target, double maxAngleDegrees, double maxDistance) {
        Vector3 toTarget = target.minus(eye);
        double distance = toTarget.length();
        if (distance > maxDistance) {
            return false;
        }
        if (distance == 0) {
            return true;
        }
        double cosine = direction.dot(toTarget) / (direction.length() * distance);
        return Math.toDegrees(Math.acos(Math.clamp(cosine, -1, 1))) <= maxAngleDegrees;
    }
}
