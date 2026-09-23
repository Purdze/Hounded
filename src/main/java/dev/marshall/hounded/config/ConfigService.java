package dev.marshall.hounded.config;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the current settings and messages. A reload swaps both at once, and only if both files
 * load, so a typo in one file never leaves the plugin half-reloaded.
 */
public final class ConfigService {
    private final ConfigLoader loader;
    private final Logger logger;
    private LoadedConfig current;

    /** Loads immediately; throws if the very first load fails, since there is nothing to fall back to. */
    public ConfigService(ConfigLoader loader, Logger logger) throws ConfigLoadException {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.current = loader.load();
    }

    /** @return true if the new files are now active; false if the previous ones were kept */
    public boolean reload() {
        try {
            current = loader.load();
            return true;
        } catch (ConfigLoadException exception) {
            logger.log(Level.SEVERE, "Reload failed, keeping the previous config and messages", exception);
            return false;
        }
    }

    public Settings settings() {
        return current.settings();
    }

    public Messages messages() {
        return current.messages();
    }
}
