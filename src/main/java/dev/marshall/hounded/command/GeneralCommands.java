package dev.marshall.hounded.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Objects;

/** {@code /hounded help} and {@code /hounded reload}. */
final class GeneralCommands {
    private final ConfigService configService;
    private final CommandReplies replies;

    GeneralCommands(ConfigService configService, CommandReplies replies) {
        this.configService = Objects.requireNonNull(configService, "configService");
        this.replies = Objects.requireNonNull(replies, "replies");
    }

    LiteralArgumentBuilder<CommandSourceStack> help() {
        return Commands.literal("help").executes(context -> showHelp(context.getSource()));
    }

    LiteralArgumentBuilder<CommandSourceStack> reload() {
        return Commands.literal("reload").executes(context -> {
            MessageKey result = configService.reload() ? MessageKey.RELOAD_SUCCESS : MessageKey.RELOAD_FAILED;
            return replies.send(context.getSource(), result);
        });
    }

    int showHelp(CommandSourceStack source) {
        return replies.send(source, MessageKey.HELP);
    }
}
