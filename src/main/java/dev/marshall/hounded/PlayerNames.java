package dev.marshall.hounded;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Server;

/** Names for players who may be offline. */
public final class PlayerNames {

    private PlayerNames() {}

    /** A player who never joined this server has no name; show the UUID rather than nothing. */
    public static String displayName(Server server, UUID player) {
        return Optional.ofNullable(server.getOfflinePlayer(player).getName()).orElse(player.toString());
    }
}
