package dev.marshall.hounded.tracking;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Where each runner last left each dimension, so a hunter there knows which portal to take. */
public final class PortalMemory {
    private final Map<UUID, Map<Dimension, Position>> exitsByRunner = new HashMap<>();

    /** @param from where the runner stood in the dimension they left */
    public void recordExit(UUID runner, DimensionalPosition from) {
        Objects.requireNonNull(runner, "runner");
        Objects.requireNonNull(from, "from");
        exitsByRunner
                .computeIfAbsent(runner, ignored -> new EnumMap<>(Dimension.class))
                .put(from.dimension(), from.position());
    }

    /** @return an immutable snapshot, empty if the runner never left a dimension */
    public Map<Dimension, Position> exitsOf(UUID runner) {
        return Map.copyOf(exitsByRunner.getOrDefault(Objects.requireNonNull(runner, "runner"), Map.of()));
    }

    public void clear() {
        exitsByRunner.clear();
    }
}
