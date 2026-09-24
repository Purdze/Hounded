package dev.marshall.hounded.tracking;

public record Position(double x, double y, double z) {

    /** Distance on the ground, ignoring height, like the coordinates a player compares on F3. */
    public double horizontalDistanceTo(Position other) {
        return Math.hypot(other.x - x, other.z - z);
    }
}
