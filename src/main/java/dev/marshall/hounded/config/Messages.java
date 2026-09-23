package dev.marshall.hounded.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Player-facing text from {@code messages.yml}, rendered with MiniMessage. Replaced as a whole on reload. */
public final class Messages {
    private final Map<MessageKey, String> templates;
    private final MiniMessage miniMessage;
    private final Logger logger;
    private final Set<MessageKey> reportedMissingKeys = new HashSet<>();

    public Messages(Map<MessageKey, String> templates, MiniMessage miniMessage, Logger logger) {
        this.templates = Map.copyOf(templates);
        this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** Renders a message without the prefix, e.g. for item names. */
    public Component render(MessageKey key, TagResolver... placeholders) {
        String template = templates.get(key);
        if (template == null) {
            // Show the key rather than nothing, so a broken translation is visible and reportable.
            if (reportedMissingKeys.add(key)) {
                logger.warning("Message '" + key.path() + "' is missing from messages.yml and the bundled defaults");
            }
            return Component.text(key.path());
        }
        return miniMessage.deserialize(template, TagResolver.resolver(placeholders));
    }

    /** Renders a chat message with the configured prefix in front. */
    public Component chat(MessageKey key, TagResolver... placeholders) {
        return render(MessageKey.PREFIX).append(render(key, placeholders));
    }
}
