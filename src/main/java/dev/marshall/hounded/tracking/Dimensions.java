package dev.marshall.hounded.tracking;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;

/** Maps Bukkit worlds and locations onto the pure tracking types. */
public final class Dimensions {

    private Dimensions() {}

    /** Empty for custom worlds, which Hounded does not track. */
    public static Optional<Dimension> of(World world) {
        return switch (world.getEnvironment()) {
            case NORMAL -> Optional.of(Dimension.OVERWORLD);
            case NETHER -> Optional.of(Dimension.NETHER);
            case THE_END -> Optional.of(Dimension.END);
            case CUSTOM -> Optional.empty();
        };
    }

    public static Optional<DimensionalPosition> positionOf(Location location) {
        return of(location.getWorld())
                .map(dimension -> new DimensionalPosition(
                        dimension, new Position(location.getX(), location.getY(), location.getZ())));
    }
}
