package dev.marshall.hounded.round;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Puts eliminated runners into spectator mode and gives them their previous mode back afterwards.
 * Players who are offline when the round ends keep a pending restore until they next join, so this
 * state deliberately survives a quit. It does not survive a server restart.
 */
final class SpectatorSwitcher {
    private final Server server;
    private final Map<UUID, GameMode> previousModes = new HashMap<>();

    SpectatorSwitcher(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    void makeSpectator(Player player) {
        previousModes.putIfAbsent(player.getUniqueId(), player.getGameMode());
        player.setGameMode(GameMode.SPECTATOR);
    }

    void restoreAll() {
        for (UUID player : List.copyOf(previousModes.keySet())) {
            Optional.ofNullable(server.getPlayer(player)).ifPresent(this::restoreIfPending);
        }
    }

    void restoreIfPending(Player player) {
        GameMode previous = previousModes.remove(player.getUniqueId());
        if (previous != null) {
            player.setGameMode(previous);
        }
    }
}
