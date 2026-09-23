package dev.marshall.hounded.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RosterTest {
    private final Roster roster = new Roster();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void assignGivesRole() {
        assertEquals(Optional.empty(), roster.assign(alice, Role.RUNNER));
        assertEquals(Optional.of(Role.RUNNER), roster.roleOf(alice));
    }

    @Test
    void assigningAnotherRoleReplacesThePreviousOne() {
        roster.assign(alice, Role.RUNNER);
        assertEquals(Optional.of(Role.RUNNER), roster.assign(alice, Role.HUNTER));
        assertEquals(List.of(alice), roster.playersWith(Role.HUNTER));
        assertFalse(roster.hasAny(Role.RUNNER));
    }

    @Test
    void unassignRemovesRole() {
        roster.assign(alice, Role.HUNTER);
        assertEquals(Optional.of(Role.HUNTER), roster.unassign(alice));
        assertEquals(Optional.empty(), roster.roleOf(alice));
        assertEquals(Optional.empty(), roster.unassign(alice));
    }

    @Test
    void clearRemovesOnlyThatRole() {
        roster.assign(alice, Role.RUNNER);
        roster.assign(bob, Role.HUNTER);
        assertEquals(1, roster.clear(Role.RUNNER));
        assertFalse(roster.hasAny(Role.RUNNER));
        assertTrue(roster.hasAny(Role.HUNTER));
    }

    @Test
    void playersAreListedInAssignmentOrder() {
        roster.assign(bob, Role.RUNNER);
        roster.assign(alice, Role.RUNNER);
        assertEquals(List.of(bob, alice), roster.playersWith(Role.RUNNER));
    }
}
