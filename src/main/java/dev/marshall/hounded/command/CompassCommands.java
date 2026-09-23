package dev.marshall.hounded.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.marshall.hounded.Permissions;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.tracking.TrackingService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Objects;
import org.bukkit.entity.Player;

/** {@code /hounded compass}: a replacement tracking compass for hunters. */
final class CompassCommands {
    private final TrackingService trackingService;
    private final CommandReplies replies;

    CompassCommands(TrackingService trackingService, CommandReplies replies) {
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
        this.replies = Objects.requireNonNull(replies, "replies");
    }

    LiteralArgumentBuilder<CommandSourceStack> compass() {
        return Commands.literal("compass")
                .requires(source -> source.getSender().hasPermission(Permissions.COMPASS))
                .executes(context -> give(context.getSource()));
    }

    private int give(CommandSourceStack source) {
        if (source.getSender() instanceof Player player && trackingService.giveCompass(player)) {
            return Command.SINGLE_SUCCESS;
        }
        return replies.send(source, MessageKey.COMPASS_NOT_IN_ROUND);
    }
}
