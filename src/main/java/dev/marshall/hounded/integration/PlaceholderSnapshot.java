package dev.marshall.hounded.integration;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Placeholder values worked out on the main thread and readable from any thread. PlaceholderAPI
 * may ask from async tasks, and the game and tracking state behind the values are not thread-safe.
 */
public final class PlaceholderSnapshot {
    private static final String NOT_APPLICABLE = "";

    private record Values(
            Map<HoundedPlaceholder, String> global, Map<UUID, Map<HoundedPlaceholder, String>> perPlayer) {}

    private final PlaceholderResolver resolver;
    private final Server server;
    private volatile Values values = new Values(Map.of(), Map.of());

    public PlaceholderSnapshot(PlaceholderResolver resolver, Server server) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.server = Objects.requireNonNull(server, "server");
    }

    /** Main thread only. Replaces all values at once, so a reader never sees half an update. */
    public void refresh() {
        Map<UUID, Map<HoundedPlaceholder, String>> perPlayer = new HashMap<>();
        for (Player player : server.getOnlinePlayers()) {
            perPlayer.put(player.getUniqueId(), resolveAll(true, Optional.of(player)));
        }
        values = new Values(resolveAll(false, Optional.empty()), Map.copyOf(perPlayer));
    }

    /**
     * Safe from any thread.
     *
     * @param player who the placeholder is for; may be null when there is no player context
     * @return empty for a name Hounded doesn't know
     */
    public Optional<String> valueFor(OfflinePlayer player, String name) {
        Values current = values;
        return HoundedPlaceholder.byName(name).map(placeholder -> {
            if (!placeholder.isPerPlayer()) {
                return current.global().getOrDefault(placeholder, NOT_APPLICABLE);
            }
            return Optional.ofNullable(player)
                    .map(known -> current.perPlayer().get(known.getUniqueId()))
                    .map(playerValues -> playerValues.get(placeholder))
                    .orElse(NOT_APPLICABLE);
        });
    }

    private Map<HoundedPlaceholder, String> resolveAll(boolean perPlayer, Optional<Player> player) {
        Map<HoundedPlaceholder, String> resolved = new EnumMap<>(HoundedPlaceholder.class);
        Arrays.stream(HoundedPlaceholder.values())
                .filter(placeholder -> placeholder.isPerPlayer() == perPlayer)
                .forEach(placeholder -> resolved.put(placeholder, resolver.resolve(placeholder, player)));
        return Map.copyOf(resolved);
    }
}
