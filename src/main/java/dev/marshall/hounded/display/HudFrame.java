package dev.marshall.hounded.display;

import java.util.List;

/**
 * What one player's display shows right now.
 *
 * @param progress how full the boss bar is, from 0 to 1
 */
public record HudFrame(List<HudLine> lines, float progress) {
    public static final HudFrame HIDDEN = new HudFrame(List.of(), 0f);

    public HudFrame {
        lines = List.copyOf(lines);
        if (progress < 0f || progress > 1f) {
            throw new IllegalArgumentException("progress must be between 0 and 1");
        }
    }

    public boolean isHidden() {
        return lines.isEmpty();
    }
}
