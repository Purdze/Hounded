package dev.marshall.hounded.round;

import java.time.Duration;

/** Formats the hunt timer as {@code M:SS}, or {@code H:MM:SS} from one hour on. */
public final class HuntTimeFormatter {

    private HuntTimeFormatter() {}

    public static String format(Duration duration) {
        long totalSeconds = Math.max(0, duration.toSeconds());
        long hours = totalSeconds / 3600;
        long minutes = totalSeconds % 3600 / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return "%d:%02d:%02d".formatted(hours, minutes, seconds);
        }
        return "%d:%02d".formatted(minutes, seconds);
    }
}
