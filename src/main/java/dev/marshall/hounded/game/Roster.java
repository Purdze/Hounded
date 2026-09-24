package dev.marshall.hounded.game;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Which player holds which role. A player holds at most one role. Not thread-safe: the plugin only
 * touches it from the main thread.
 */
public final class Roster {
    private final Map<Role, Set<UUID>> playersByRole = new EnumMap<>(Role.class);

    public Roster() {
        for (Role role : Role.values()) {
            // LinkedHashSet keeps assignment order, so "cycle through runners" is predictable.
            playersByRole.put(role, new LinkedHashSet<>());
        }
    }

    /**
     * Gives {@code player} the role, replacing any other role they held.
     *
     * @return the role the player held before, if any
     */
    public Optional<Role> assign(UUID player, Role role) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(role, "role");
        Optional<Role> previous = unassign(player);
        playersByRole.get(role).add(player);
        return previous;
    }

    /** @return the role the player held, if any */
    public Optional<Role> unassign(UUID player) {
        Objects.requireNonNull(player, "player");
        for (Map.Entry<Role, Set<UUID>> entry : playersByRole.entrySet()) {
            if (entry.getValue().remove(player)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public Optional<Role> roleOf(UUID player) {
        Objects.requireNonNull(player, "player");
        return playersByRole.entrySet().stream()
                .filter(entry -> entry.getValue().contains(player))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /** @return an immutable snapshot, in assignment order */
    public List<UUID> playersWith(Role role) {
        return List.copyOf(playersByRole.get(Objects.requireNonNull(role, "role")));
    }

    public boolean hasAny(Role role) {
        return !playersByRole.get(Objects.requireNonNull(role, "role")).isEmpty();
    }
}
