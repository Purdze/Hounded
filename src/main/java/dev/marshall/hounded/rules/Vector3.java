package dev.marshall.hounded.rules;

/** A point or direction in space, without Bukkit types, so the gaze maths is unit-testable. */
public record Vector3(double x, double y, double z) {

    public Vector3 minus(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double length() {
        return Math.sqrt(dot(this));
    }
}
