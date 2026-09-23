package dev.marshall.hounded.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code /hounded runner|hunter add|remove|list|clear}, built once per role. */
final class RoleCommands {
    private static final String PLAYER_ARGUMENT = "player";

    private final GameSession session;
    private final CommandReplies replies;
    private final Server server;

    RoleCommands(GameSession session, CommandReplies replies, Server server) {
        this.session = Objects.requireNonNull(session, "session");
        this.replies = Objects.requireNonNull(replies, "replies");
        this.server = Objects.requireNonNull(server, "server");
    }

    LiteralArgumentBuilder<CommandSourceStack> build(Role role) {
        return Commands.literal(role.name().toLowerCase(Locale.ROOT))
                .then(Commands.literal("add")
                        .then(Commands.argument(PLAYER_ARGUMENT, ArgumentTypes.player())
                                .executes(context -> add(context, role))))
                .then(Commands.literal("remove")
                        .then(Commands.argument(PLAYER_ARGUMENT, ArgumentTypes.player())
                                .executes(context -> remove(context, role))))
                .then(Commands.literal("list").executes(context -> list(context.getSource(), role)))
                .then(Commands.literal("clear").executes(context -> clear(context.getSource(), role)));
    }

    private int add(CommandContext<CommandSourceStack> context, Role role) throws CommandSyntaxException {
        Player target = targetPlayer(context);
        return replies.reply(
                context.getSource(),
                session.assignRole(target.getUniqueId(), role),
                MessageKey.ROLE_ASSIGNED,
                playerAndRole(target, role));
    }

    private int remove(CommandContext<CommandSourceStack> context, Role role) throws CommandSyntaxException {
        Player target = targetPlayer(context);
        return replies.reply(
                context.getSource(),
                session.unassignRole(target.getUniqueId(), role),
                MessageKey.ROLE_REMOVED,
                playerAndRole(target, role));
    }

    private int list(CommandSourceStack source, Role role) {
        List<UUID> players = session.playersWith(role);
        if (players.isEmpty()) {
            return replies.send(source, MessageKey.ROLE_LIST_EMPTY, replies.roleNames(role));
        }
        Component names = Component.join(
                JoinConfiguration.commas(true),
                players.stream().map(this::displayName).map(Component::text).toList());
        return replies.send(
                source,
                MessageKey.ROLE_LIST,
                replies.roleNames(role),
                Placeholder.component(PlaceholderNames.PLAYERS, names));
    }

    private int clear(CommandSourceStack source, Role role) {
        return replies.reply(source, session.clearRole(role), MessageKey.ROLE_CLEARED, replies.roleNames(role));
    }

    private static Player targetPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return context.getArgument(PLAYER_ARGUMENT, PlayerSelectorArgumentResolver.class)
                .resolve(context.getSource())
                .getFirst();
    }

    private TagResolver playerAndRole(Player player, Role role) {
        return TagResolver.resolver(
                Placeholder.unparsed(PlaceholderNames.PLAYER, player.getName()), replies.roleNames(role));
    }

    /** A player who never joined this server has no name; show the UUID rather than nothing. */
    private String displayName(UUID player) {
        return Optional.ofNullable(server.getOfflinePlayer(player).getName()).orElse(player.toString());
    }
}
