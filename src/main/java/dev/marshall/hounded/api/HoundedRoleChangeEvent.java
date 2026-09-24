package dev.marshall.hounded.api;

import dev.marshall.hounded.game.Role;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread just before a player's role changes: added, moved to the other side, or
 * removed. Clearing a role fires one event per player; cancelling one keeps just that player. Not
 * fired during a round, because roles are locked then.
 */
public final class HoundedRoleChangeEvent extends HoundedCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final Optional<Role> from;
    private final Optional<Role> to;

    /**
     * Creates the event. Only Hounded fires it.
     *
     * @param player the player whose role changes
     * @param from their role before; empty if they had none
     * @param to their role after; empty if it is being removed
     */
    public HoundedRoleChangeEvent(UUID player, Optional<Role> from, Optional<Role> to) {
        this.player = Objects.requireNonNull(player, "player");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
    }

    /** @return the id of the player whose role changes; they may be offline when a role is cleared */
    public UUID player() {
        return player;
    }

    /** @return the role before the change; empty if the player had none */
    public Optional<Role> from() {
        return from;
    }

    /** @return the role after the change; empty if the role is being removed */
    public Optional<Role> to() {
        return to;
    }

    /** @return the handlers for this event */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handlers for this event; required by Bukkit */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
