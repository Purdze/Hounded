package dev.marshall.hounded.integration;

import java.util.Arrays;
import java.util.Optional;

/** The placeholders Hounded offers, as {@code %hounded_<name>%}. */
public enum HoundedPlaceholder {
    ROLE("role"),
    STATE("state"),
    TIMER("timer"),
    HEADSTART("headstart"),
    DISTANCE("distance"),
    TARGET("target"),
    RUNNERS_LEFT("runners_left");

    private final String placeholderName;

    HoundedPlaceholder(String placeholderName) {
        this.placeholderName = placeholderName;
    }

    public static Optional<HoundedPlaceholder> byName(String name) {
        return Arrays.stream(values())
                .filter(placeholder -> placeholder.placeholderName.equalsIgnoreCase(name))
                .findFirst();
    }
}
