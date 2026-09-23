package dev.marshall.hounded.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** Guards against the bundled files drifting away from the key constants and defaults. */
class BundledResourcesTest {

    private static YamlConfiguration loadBundled(String name) throws IOException {
        try (InputStream stream = BundledResourcesTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(stream, name + " is not on the classpath");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void bundledConfigParsesToDefaultsWithoutWarnings() throws IOException {
        YamlConfiguration config = loadBundled(ConfigLoader.CONFIG_FILE);

        SettingsParser.Result result = new SettingsParser().parse(config.getValues(true));

        assertTrue(result.warnings().isEmpty(), () -> result.warnings().toString());
        assertEquals(Settings.DEFAULTS, result.settings());
    }

    @Test
    void everyMessageKeyExistsAndIsValidMiniMessage() throws IOException {
        YamlConfiguration messages = loadBundled(ConfigLoader.MESSAGES_FILE);
        MiniMessage strict = MiniMessage.builder().strict(true).build();

        for (String key : MessageKeys.ALL) {
            String template = messages.getString(key);
            assertNotNull(template, "messages.yml is missing " + key);
            // Strict mode fails on unclosed tags, which would otherwise bleed colour into later text.
            strict.deserialize(template);
        }
    }

    @Test
    void placeholdersAreFilledIn() {
        Messages messages = new Messages(
                Map.of(MessageKeys.ROLE_ASSIGNED, "<player> is now a <role>."),
                MiniMessage.miniMessage(),
                Logger.getAnonymousLogger());

        Component rendered = messages.render(
                MessageKeys.ROLE_ASSIGNED,
                Placeholder.unparsed("player", "Steve"),
                Placeholder.unparsed("role", "runner"));

        assertEquals(
                "Steve is now a runner.",
                PlainTextComponentSerializer.plainText().serialize(rendered));
    }

    @Test
    void missingMessageRendersItsKey() {
        Messages messages = new Messages(Map.of(), MiniMessage.miniMessage(), Logger.getAnonymousLogger());

        Component rendered = messages.render(MessageKeys.WIN_RUNNERS);

        assertEquals(
                MessageKeys.WIN_RUNNERS,
                PlainTextComponentSerializer.plainText().serialize(rendered));
    }
}
