package dev.marshall.hounded.display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Server;

/** A display that keeps one {@code T} per player showing it. */
abstract class PerPlayerHud<T> implements Hud {
    protected final Server server;
    protected final Map<UUID, T> shownTo = new HashMap<>();

    PerPlayerHud(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public final void hideAll() {
        for (UUID viewer : List.copyOf(shownTo.keySet())) {
            Optional.ofNullable(server.getPlayer(viewer)).ifPresent(this::hide);
        }
        shownTo.clear();
    }
}
