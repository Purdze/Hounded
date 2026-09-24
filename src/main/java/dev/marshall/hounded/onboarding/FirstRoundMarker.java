package dev.marshall.hounded.onboarding;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Remembers across restarts whether a round has ever been started, so the quick-start guide stops
 * once the server owner has figured it out. Deleting {@code data.yml} shows the guide again.
 */
public final class FirstRoundMarker {
    static final String DATA_FILE = "data.yml";
    static final String ROUND_PLAYED_KEY = "first-round-played";

    private final Plugin plugin;
    private final File file;
    private boolean roundPlayed;

    /** Reads {@code data.yml} once; a tiny synchronous read at enable, like the config. */
    public FirstRoundMarker(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.file = new File(plugin.getDataFolder(), DATA_FILE);
        this.roundPlayed = readRoundPlayed();
    }

    public boolean hasPlayedARound() {
        return roundPlayed;
    }

    /** Saves off the main thread, and only the first time. */
    public void markRoundPlayed() {
        if (roundPlayed) {
            return;
        }
        roundPlayed = true;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    private boolean readRoundPlayed() {
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger()
                    .log(
                            Level.WARNING,
                            "Could not read " + file.getPath() + "; the quick-start guide will show",
                            exception);
            return false;
        }
        return data.getBoolean(ROUND_PLAYED_KEY);
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set(ROUND_PLAYED_KEY, true);
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not save " + file.getPath(), exception);
        }
    }
}
