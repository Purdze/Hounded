package dev.marshall.hounded.display;

import dev.marshall.hounded.config.MessageKey;
import java.util.Map;
import java.util.Objects;

/**
 * One line of the display, still untranslated.
 *
 * @param values placeholders filled with plain text
 * @param nested placeholders filled with another message, e.g. a translatable dimension name
 */
public record HudLine(MessageKey key, Map<String, String> values, Map<String, MessageKey> nested) {

    public HudLine {
        Objects.requireNonNull(key, "key");
        values = Map.copyOf(values);
        nested = Map.copyOf(nested);
    }

    static HudLine of(MessageKey key, Map<String, String> values) {
        return new HudLine(key, values, Map.of());
    }
}
