package dev.marshall.hounded.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.testing.PluginFixture;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private static Messages messagesWith(Map<MessageKey, String> templates) {
        return new Messages(templates, MiniMessage.miniMessage(), Logger.getAnonymousLogger());
    }

    @Test
    void placeholdersAreFilledIn() {
        Messages messages = messagesWith(Map.of(MessageKey.ROLE_ASSIGNED, "<player> is now a <role>."));

        Component rendered = messages.render(
                MessageKey.ROLE_ASSIGNED,
                Placeholder.unparsed("player", "Steve"),
                Placeholder.unparsed("role", "runner"));

        assertEquals("Steve is now a runner.", PluginFixture.plain(rendered));
    }

    @Test
    void chatPutsPrefixInFront() {
        Messages messages = messagesWith(Map.of(MessageKey.PREFIX, "[H] ", MessageKey.STOP_STOPPED, "Stopped."));

        assertEquals("[H] Stopped.", PluginFixture.plain(messages.chat(MessageKey.STOP_STOPPED)));
    }

    @Test
    void missingMessageRendersItsPath() {
        Messages messages = messagesWith(Map.of());

        assertEquals(MessageKey.WIN_RUNNERS.path(), PluginFixture.plain(messages.render(MessageKey.WIN_RUNNERS)));
    }
}
