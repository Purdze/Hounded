package dev.marshall.hounded.command;

import com.mojang.brigadier.Command;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
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

    /** Sends {@code successKey}, or the rejection's message if the change was refused. */
    int reply(CommandSourceStack source, TransitionResult result, MessageKey successKey, TagResolver... placeholders) {
        MessageKey key = result instanceof TransitionResult.Rejected rejected
                ? RejectionMessages.keyFor(rejected.reason())
                : successKey;
        return send(source, key, placeholders);
    }

    /** Only speaks up on rejection, for actions whose success is already broadcast to everyone. */
    int replyIfRejected(CommandSourceStack source, TransitionResult result) {
        if (result instanceof TransitionResult.Rejected rejected) {
            send(source, RejectionMessages.keyFor(rejected.reason()));
        }
        return Command.SINGLE_SUCCESS;
    }

    /** Both {@code <role>} and {@code <roles>}, rendered from the current messages. */
    TagResolver roleNames(Role role) {
        MessageKey singular = switch (role) {
            case RUNNER -> MessageKey.ROLE_NAME_RUNNER;
            case HUNTER -> MessageKey.ROLE_NAME_HUNTER;
        };
        MessageKey plural = switch (role) {
            case RUNNER -> MessageKey.ROLE_NAME_RUNNERS;
            case HUNTER -> MessageKey.ROLE_NAME_HUNTERS;
        };
        return TagResolver.resolver(
                Placeholder.component(
                        PlaceholderNames.ROLE, configService.messages().render(singular)),
                Placeholder.component(
                        PlaceholderNames.ROLES, configService.messages().render(plural)));
    }
}
