package dev.marshall.hounded.tracking;

import java.util.Objects;

/** A position together with the dimension it is in. */
public record DimensionalPosition(Dimension dimension, Position position) {
    public DimensionalPosition {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
    }
}
