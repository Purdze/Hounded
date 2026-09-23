package dev.marshall.hounded.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.marshall.hounded.Permissions;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.round.RoundService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Server;

/** The {@code /hounded} command tree. Admin branches are hidden from players without permission. */
public final class HoundedCommand {
    private static final String NAME = "hounded";

    private final GeneralCommands general;
    private final RoleCommands roles;
    private final RoundCommands round;

    public HoundedCommand(GameSession session, RoundService roundService, ConfigService configService, Server server) {
        CommandReplies replies = new CommandReplies(configService);
        this.general = new GeneralCommands(configService, replies);
        this.roles = new RoleCommands(session, replies, server);
        this.round = new RoundCommands(roundService, configService, replies);
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .executes(context -> general.showHelp(context.getSource()))
                .then(general.help())
                .then(adminOnly(roles.build(Role.RUNNER)))
                .then(adminOnly(roles.build(Role.HUNTER)))
                .then(adminOnly(round.start()))
                .then(adminOnly(round.stop()))
                .then(adminOnly(general.reload()))
                .build();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminOnly(
            LiteralArgumentBuilder<CommandSourceStack> branch) {
        return branch.requires(source -> source.getSender().hasPermission(Permissions.ADMIN));
    }
}
