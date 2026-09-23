package dev.marshall.hounded.round;

import java.time.Duration;

/** Converts between real time and server ticks. */
final class Ticks {
    static final long PER_SECOND = 20L;
    private static final long MILLIS_PER_TICK = 1000L / PER_SECOND;

    private Ticks() {}

    static int from(Duration duration) {
        return Math.toIntExact(Math.max(0, duration.toMillis()) / MILLIS_PER_TICK);
    }
}
