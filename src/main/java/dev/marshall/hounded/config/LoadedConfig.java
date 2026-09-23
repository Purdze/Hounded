package dev.marshall.hounded.config;

import java.util.Objects;

/** One consistent generation of settings and messages. */
public record LoadedConfig(Settings settings, Messages messages) {
    public LoadedConfig {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(messages, "messages");
    }
}
