package dev.marshall.hounded.role;

import dev.marshall.hounded.api.HoundedRoleChangeEvent;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.RejectionReason;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.game.TransitionResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.plugin.PluginManager;

/**
 * Changes roles on behalf of commands, letting other plugins veto each change through {@link
 * HoundedRoleChangeEvent}. The event only fires for a change that would otherwise go ahead.
 */
public final class RoleService {
    private final GameSession session;
    private final PluginManager pluginManager;

    public RoleService(GameSession session, PluginManager pluginManager) {
        this.session = Objects.requireNonNull(session, "session");
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
    }

    public List<UUID> playersWith(Role role) {
        return session.playersWith(role);
    }

    /** Gives the player {@code role}, moving them from the other side if needed. */
    public TransitionResult assign(UUID player, Role role) {
        return whenUnlocked(() -> {
            Optional<Role> current = session.roleOf(player);
            if (current.equals(Optional.of(role))) {
                return new TransitionResult.Unchanged(session.state());
            }
            return unlessVetoed(player, current, Optional.of(role), () -> session.assignRole(player, role));
        });
    }

    public TransitionResult unassign(UUID player, Role role) {
        return whenUnlocked(() -> {
            if (!session.hasRole(player, role)) {
                return new TransitionResult.Rejected(RejectionReason.NOT_IN_ROLE);
            }
            return unlessVetoed(player, Optional.of(role), Optional.empty(), () -> session.unassignRole(player, role));
        });
    }

    /** Removes {@code role} from everyone who has it, except players whose change was vetoed. */
    public TransitionResult clear(Role role) {
        return whenUnlocked(() -> {
            for (UUID player : session.playersWith(role)) {
                if (!vetoed(player, Optional.of(role), Optional.empty())) {
                    session.unassignRole(player, role);
                }
            }
            return new TransitionResult.Unchanged(session.state());
        });
    }

    /** Checked first so no event fires for a change the session would refuse anyway. */
    private TransitionResult whenUnlocked(Supplier<TransitionResult> change) {
        if (session.rolesLocked()) {
            return new TransitionResult.Rejected(RejectionReason.ROLES_LOCKED);
        }
        return change.get();
    }

    private TransitionResult unlessVetoed(
            UUID player, Optional<Role> from, Optional<Role> to, Supplier<TransitionResult> change) {
        if (vetoed(player, from, to)) {
            return new TransitionResult.Rejected(RejectionReason.CANCELLED_BY_PLUGIN);
        }
        return change.get();
    }

    private boolean vetoed(UUID player, Optional<Role> from, Optional<Role> to) {
        HoundedRoleChangeEvent event = new HoundedRoleChangeEvent(player, from, to);
        pluginManager.callEvent(event);
        return event.isCancelled();
    }
}
