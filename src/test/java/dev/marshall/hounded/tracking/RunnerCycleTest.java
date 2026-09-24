package dev.marshall.hounded.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunnerCycleTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final List<UUID> remaining = List.of(first, second, third);

    @Test
    void currentIsTheChosenRunnerWhileTheyAreStillIn() {
        assertEquals(Optional.of(second), RunnerCycle.current(remaining, Optional.of(second)));
    }

    @Test
    void currentFallsBackToTheFirstRunner() {
        assertEquals(Optional.of(first), RunnerCycle.current(remaining, Optional.empty()));
        assertEquals(Optional.of(first), RunnerCycle.current(remaining, Optional.of(UUID.randomUUID())));
    }

    @Test
    void nextMovesAlongAndWrapsAround() {
        assertEquals(Optional.of(second), RunnerCycle.next(remaining, Optional.of(first)));
        assertEquals(Optional.of(first), RunnerCycle.next(remaining, Optional.of(third)));
    }

    @Test
    void nextStartsAtTheFirstWhenNothingValidIsChosen() {
        assertEquals(Optional.of(first), RunnerCycle.next(remaining, Optional.empty()));
        assertEquals(Optional.of(first), RunnerCycle.next(remaining, Optional.of(UUID.randomUUID())));
    }

    @Test
    void noRunnersMeansNothingToTrack() {
        assertEquals(Optional.empty(), RunnerCycle.current(List.of(), Optional.of(first)));
        assertEquals(Optional.empty(), RunnerCycle.next(List.of(), Optional.of(first)));
    }
}
