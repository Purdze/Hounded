package dev.marshall.hounded.integration;

import java.util.Arrays;
import java.util.Optional;

/** The placeholders Hounded offers, as {@code %hounded_<name>%}. */
public enum HoundedPlaceholder {
    ROLE("role", true),
    STATE("state", false),
    TIMER("timer", false),
    HEADSTART("headstart", false),
    DISTANCE("distance", true),
    TARGET("target", true),
    RUNNERS_LEFT("runners_left", false);

    private final String placeholderName;
    private final boolean perPlayer;

    HoundedPlaceholder(String placeholderName, boolean perPlayer) {
        this.placeholderName = placeholderName;
        this.perPlayer = perPlayer;
    }

    public boolean isPerPlayer() {
        return perPlayer;
    }

    public static Optional<HoundedPlaceholder> byName(String name) {
        return Arrays.stream(values())
                .filter(placeholder -> placeholder.placeholderName.equalsIgnoreCase(name))
                .findFirst();
    }
}
