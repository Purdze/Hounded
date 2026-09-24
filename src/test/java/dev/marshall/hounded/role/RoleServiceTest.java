package dev.marshall.hounded.role;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.api.HoundedRoleChangeEvent;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.RejectionReason;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.game.TransitionResult;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleServiceTest {
    private final GameSession session = new GameSession(new MutableClock());
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private PluginFixture fixture;
    private RoleService roles;
    private List<HoundedRoleChangeEvent> events;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        roles = new RoleService(session, fixture.server().getPluginManager());
        events = fixture.captureEvents(HoundedRoleChangeEvent.class);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    /** Only the parts of an event that describe the change, for easy comparison. */
    private record Change(UUID player, Optional<Role> from, Optional<Role> to) {}

    private List<Change> changes() {
        return events.stream()
                .map(event -> new Change(event.player(), event.from(), event.to()))
                .toList();
    }

    private static TransitionResult unchanged() {
        return new TransitionResult.Unchanged(GameState.LOBBY);
    }

    private static TransitionResult rejected(RejectionReason reason) {
        return new TransitionResult.Rejected(reason);
    }

    @Test
    void addingAndMovingAPlayerAreAnnounced() {
        roles.assign(alice, Role.HUNTER);
        roles.assign(alice, Role.RUNNER);

        assertEquals(
                List.of(
                        new Change(alice, Optional.empty(), Optional.of(Role.HUNTER)),
                        new Change(alice, Optional.of(Role.HUNTER), Optional.of(Role.RUNNER))),
                changes());
        assertEquals(Optional.of(Role.RUNNER), session.roleOf(alice));
    }

    @Test
    void removingIsAnnounced() {
        roles.assign(alice, Role.RUNNER);
        events.clear();

        assertEquals(unchanged(), roles.unassign(alice, Role.RUNNER));

        assertEquals(List.of(new Change(alice, Optional.of(Role.RUNNER), Optional.empty())), changes());
    }

    @Test
    void clearingAnnouncesEachPlayer() {
        roles.assign(alice, Role.HUNTER);
        roles.assign(bob, Role.HUNTER);
        events.clear();

        roles.clear(Role.HUNTER);

        assertEquals(
                List.of(
                        new Change(alice, Optional.of(Role.HUNTER), Optional.empty()),
                        new Change(bob, Optional.of(Role.HUNTER), Optional.empty())),
                changes());
        assertEquals(List.of(), session.playersWith(Role.HUNTER));
    }

    @Test
    void nothingIsAnnouncedForChangesThatWouldNotHappen() {
        roles.assign(alice, Role.RUNNER);
        roles.assign(bob, Role.HUNTER);
        events.clear();

        assertEquals(unchanged(), roles.assign(alice, Role.RUNNER));
        assertEquals(rejected(RejectionReason.NOT_IN_ROLE), roles.unassign(bob, Role.RUNNER));
        session.start(0);
        assertEquals(rejected(RejectionReason.ROLES_LOCKED), roles.assign(UUID.randomUUID(), Role.HUNTER));

        assertEquals(List.of(), changes());
    }

    @Test
    void anotherPluginCanVetoAChange() {
        fixture.cancelEvents(HoundedRoleChangeEvent.class);

        assertEquals(rejected(RejectionReason.CANCELLED_BY_PLUGIN), roles.assign(alice, Role.RUNNER));

        assertEquals(Optional.empty(), session.roleOf(alice));
    }

    @Test
    void vetoingOnePlayerDuringAClearKeepsJustThatPlayer() {
        roles.assign(alice, Role.HUNTER);
        roles.assign(bob, Role.HUNTER);
        fixture.onEvent(HoundedRoleChangeEvent.class, EventPriority.NORMAL, event -> {
            if (event.player().equals(alice)) {
                event.setCancelled(true);
            }
        });

        roles.clear(Role.HUNTER);

        assertEquals(List.of(alice), session.playersWith(Role.HUNTER));
    }
}
