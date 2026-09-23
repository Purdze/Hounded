package dev.marshall.hounded.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.round.RoundService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Objects;

/** {@code /hounded start [seconds]} and {@code /hounded stop}. Success is broadcast by {@link RoundService}. */
final class RoundCommands {
    private static final String SECONDS_ARGUMENT = "seconds";

    private final RoundService roundService;
    private final ConfigService configService;
    private final CommandReplies replies;

    RoundCommands(RoundService roundService, ConfigService configService, CommandReplies replies) {
        this.roundService = Objects.requireNonNull(roundService, "roundService");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.replies = Objects.requireNonNull(replies, "replies");
    }

    LiteralArgumentBuilder<CommandSourceStack> start() {
        return Commands.literal("start")
                .executes(context -> start(
                        context.getSource(),
                        configService.settings().headstart().defaultSeconds()))
                .then(Commands.argument(SECONDS_ARGUMENT, IntegerArgumentType.integer(0))
                        .executes(context ->
                                start(context.getSource(), IntegerArgumentType.getInteger(context, SECONDS_ARGUMENT))));
    }

    LiteralArgumentBuilder<CommandSourceStack> stop() {
        return Commands.literal("stop")
                .executes(context -> replies.replyIfRejected(context.getSource(), roundService.stop()));
    }

    private int start(CommandSourceStack source, int headstartSeconds) {
        return replies.replyIfRejected(source, roundService.start(headstartSeconds));
    }
}
