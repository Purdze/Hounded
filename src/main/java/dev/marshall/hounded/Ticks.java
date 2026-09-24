package dev.marshall.hounded;

import java.time.Duration;

/** Converts between real time and server ticks. */
public final class Ticks {
    public static final long PER_SECOND = 20L;
    private static final long MILLIS_PER_TICK = 1000L / PER_SECOND;

    private Ticks() {}

    /** Saturates at {@link Integer#MAX_VALUE} rather than overflowing. */
    public static int from(Duration duration) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, duration.toMillis()) / MILLIS_PER_TICK);
    }
}
