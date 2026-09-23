package dev.marshall.hounded.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortalMemoryTest {
    private final PortalMemory memory = new PortalMemory();
    private final UUID runner = UUID.randomUUID();

    private static DimensionalPosition at(Dimension dimension, double x) {
        return new DimensionalPosition(dimension, new Position(x, 64, 0));
    }

    @Test
    void remembersTheExitPerDimension() {
        memory.recordExit(runner, at(Dimension.OVERWORLD, 10));
        memory.recordExit(runner, at(Dimension.NETHER, 2));

        assertEquals(
                Map.of(Dimension.OVERWORLD, new Position(10, 64, 0), Dimension.NETHER, new Position(2, 64, 0)),
                memory.exitsOf(runner));
    }

    @Test
    void latestExitFromADimensionWins() {
        memory.recordExit(runner, at(Dimension.OVERWORLD, 10));
        memory.recordExit(runner, at(Dimension.OVERWORLD, 99));

        assertEquals(Map.of(Dimension.OVERWORLD, new Position(99, 64, 0)), memory.exitsOf(runner));
    }

    @Test
    void runnersAreRememberedSeparately() {
        memory.recordExit(runner, at(Dimension.OVERWORLD, 10));

        assertEquals(Map.of(), memory.exitsOf(UUID.randomUUID()));
    }

    @Test
    void clearForgetsEverything() {
        memory.recordExit(runner, at(Dimension.OVERWORLD, 10));

        memory.clear();

        assertEquals(Map.of(), memory.exitsOf(runner));
    }
}
