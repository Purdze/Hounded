package dev.marshall.hounded.tracking;

import java.util.Objects;

public record DimensionalPosition(Dimension dimension, Position position) {
    public DimensionalPosition {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
    }
}
