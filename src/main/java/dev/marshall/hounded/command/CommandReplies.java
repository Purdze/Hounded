package dev.marshall.hounded.command;

import com.mojang.brigadier.Command;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.Messages;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.game.TransitionResult;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Objects;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Sends command feedback. Messages are looked up on every reply, so {@code /hounded reload}
 * applies immediately.
 */
final class CommandReplies {
    private final ConfigService configService;

    CommandReplies(ConfigService configService) {
        this.configService = Objects.requireNonNull(configService, "configService");
    }

    /** @return {@link Command#SINGLE_SUCCESS}; failures are reported as messages, not errors */
    int send(CommandSourceStack source, MessageKey key, TagResolver... placeholders) {
        source.getSender().sendMessage(configService.messages().chat(key, placeholders));
        return Command.SINGLE_SUCCESS;
    }

    int reply(CommandSourceStack source, TransitionResult result, MessageKey successKey, TagResolver... placeholders) {
        return send(source, RejectionMessages.keyFor(result).orElse(successKey), placeholders);
    }

    /** Only speaks up on rejection, for actions whose success is already broadcast to everyone. */
    int replyIfRejected(CommandSourceStack source, TransitionResult result) {
        RejectionMessages.keyFor(result).ifPresent(key -> send(source, key));
        return Command.SINGLE_SUCCESS;
    }

    /** Both {@code <role>} and {@code <roles>}, rendered from the current messages. */
    TagResolver roleNames(Role role) {
        RoleNameKeys keys = RoleNameKeys.of(role);
        Messages messages = configService.messages();
        return TagResolver.resolver(
                Placeholder.component(PlaceholderNames.ROLE, messages.render(keys.singular())),
                Placeholder.component(PlaceholderNames.ROLES, messages.render(keys.plural())));
    }

    private record RoleNameKeys(MessageKey singular, MessageKey plural) {
        static RoleNameKeys of(Role role) {
            return switch (role) {
                case RUNNER -> new RoleNameKeys(MessageKey.ROLE_NAME_RUNNER, MessageKey.ROLE_NAME_RUNNERS);
                case HUNTER -> new RoleNameKeys(MessageKey.ROLE_NAME_HUNTER, MessageKey.ROLE_NAME_HUNTERS);
            };
        }
    }
}
