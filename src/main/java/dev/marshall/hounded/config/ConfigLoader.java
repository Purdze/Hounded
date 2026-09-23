package dev.marshall.hounded.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/** Reads {@code config.yml} and {@code messages.yml} from the plugin folder, writing defaults on first run. */
public final class ConfigLoader {
    public static final String CONFIG_FILE = "config.yml";
    public static final String MESSAGES_FILE = "messages.yml";

    private final Plugin plugin;
    private final SettingsParser settingsParser = new SettingsParser();

    public ConfigLoader(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Blocking file I/O: call on enable or from an explicit reload, never every tick. */
    public LoadedConfig load() throws ConfigLoadException {
        saveDefaultIfAbsent(CONFIG_FILE);
        saveDefaultIfAbsent(MESSAGES_FILE);

        YamlConfiguration config = loadYaml(CONFIG_FILE);
        SettingsParser.Result parsed = settingsParser.parse(config.getValues(true));
        parsed.warnings().forEach(warning -> plugin.getLogger().warning(CONFIG_FILE + ": " + warning));

        YamlConfiguration messagesFile = loadYaml(MESSAGES_FILE);
        // Bundled defaults fill in keys added in newer versions that the user's file does not have yet.
        messagesFile.setDefaults(loadBundledYaml(MESSAGES_FILE));
        Map<MessageKey, String> templates = new EnumMap<>(MessageKey.class);
        for (MessageKey key : MessageKey.values()) {
            String template = messagesFile.getString(key.path());
            if (template != null) {
                templates.put(key, template);
            }
        }
        Messages messages = new Messages(templates, MiniMessage.miniMessage(), plugin.getLogger());
        return new LoadedConfig(parsed.settings(), messages);
    }

    private void saveDefaultIfAbsent(String fileName) {
        if (!new File(plugin.getDataFolder(), fileName).exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private YamlConfiguration loadYaml(String fileName) throws ConfigLoadException {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new ConfigLoadException(
                    "Could not read " + file.getPath() + ": " + exception.getMessage(), exception);
        }
        return yaml;
    }

    private YamlConfiguration loadBundledYaml(String fileName) throws ConfigLoadException {
        try (InputStream stream = plugin.getResource(fileName)) {
            if (stream == null) {
                throw new ConfigLoadException("Bundled " + fileName + " is missing from the plugin jar", null);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new ConfigLoadException("Could not read bundled " + fileName, exception);
        }
    }
}
