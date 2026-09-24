package dev.marshall.hounded.tracking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Which runner a hunter's compass follows, and which one comes next when they switch. */
public final class RunnerCycle {

    private RunnerCycle() {}

    /** The chosen runner if they are still in the round, otherwise the first one who is. */
    public static Optional<UUID> current(List<UUID> remaining, Optional<UUID> chosen) {
        return chosen.filter(remaining::contains).or(() -> remaining.stream().findFirst());
    }

    /** The runner after {@code chosen}, wrapping around; the first one if nothing valid is chosen. */
    public static Optional<UUID> next(List<UUID> remaining, Optional<UUID> chosen) {
        if (remaining.isEmpty()) {
            return Optional.empty();
        }
        int index = chosen.map(remaining::indexOf).orElse(-1);
        return Optional.of(remaining.get((index + 1) % remaining.size()));
    }
}
