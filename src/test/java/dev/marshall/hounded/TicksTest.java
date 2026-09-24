package dev.marshall.hounded;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TicksTest {

    @Test
    void convertsRealTimeToTicks() {
        assertEquals(20, Ticks.from(Duration.ofSeconds(1)));
        assertEquals(0, Ticks.from(Duration.ofMillis(-5)));
    }

    @Test
    void saturatesInsteadOfOverflowing() {
        assertEquals(Integer.MAX_VALUE, Ticks.from(Duration.ofDays(365 * 100)));
    }
}
