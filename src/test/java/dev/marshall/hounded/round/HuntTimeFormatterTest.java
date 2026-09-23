package dev.marshall.hounded.round;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class HuntTimeFormatterTest {

    @Test
    void underAnHourShowsMinutesAndSeconds() {
        assertEquals("0:00", HuntTimeFormatter.format(Duration.ZERO));
        assertEquals("0:07", HuntTimeFormatter.format(Duration.ofSeconds(7)));
        assertEquals("59:59", HuntTimeFormatter.format(Duration.ofSeconds(3599)));
    }

    @Test
    void fromAnHourShowsHours() {
        assertEquals("1:00:00", HuntTimeFormatter.format(Duration.ofHours(1)));
        assertEquals("2:03:04", HuntTimeFormatter.format(Duration.ofSeconds(2 * 3600 + 3 * 60 + 4)));
    }

    @Test
    void dropsFractionsOfASecond() {
        assertEquals("0:01", HuntTimeFormatter.format(Duration.ofMillis(1999)));
    }

    @Test
    void negativeDurationShowsZero() {
        assertEquals("0:00", HuntTimeFormatter.format(Duration.ofSeconds(-5)));
    }
}
