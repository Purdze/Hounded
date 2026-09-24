package dev.marshall.hounded.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.listener.RulesListener;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * The gaze freeze on its own session, with line of sight faked: MockBukkit doesn't simulate
 * {@code hasLineOfSight}, so the real ray trace is checked in-game.
 */
class RuleEnforcerTest {
    private static final float FACING_EAST = -90;
    private static final float FACING_WEST = 90;

    private final GameSession session = new GameSession(new MutableClock());
    private final AtomicBoolean clearView = new AtomicBoolean(true);
    private PluginFixture fixture;
    private RuleEnforcer rules;
    private World world;
    private PlayerMock runner;
    private PlayerMock hunter;

    @BeforeEach
    void setUp() throws ConfigLoadException, IOException {
        fixture = PluginFixture.start();
        world = fixture.addWorld("world", World.Environment.NORMAL);
        runner = fixture.server().addPlayer("Runner");
        hunter = fixture.server().addPlayer("Hunter");
        session.assignRole(runner.getUniqueId(), Role.RUNNER);
        session.assignRole(hunter.getUniqueId(), Role.HUNTER);
        fixture.setConfig(ConfigKey.RULES_FREEZE_WHEN_LOOKED_AT, true);
        rules = new RuleEnforcer(session, fixture.config(), fixture.server(), (looker, target) -> clearView.get());
        runnerFaces(FACING_EAST);
        hunter.teleport(new Location(world, 10, 64, 0));
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private void runnerFaces(float yaw) {
        runner.teleport(new Location(world, 0, 64, 0, yaw, 0));
    }

    @Test
    void hunterInTheRunnersCrosshairIsHeld() {
        session.start(0);

        assertTrue(rules.isHeldByGaze(hunter));
    }

    @Test
    void hunterIsFreeWhenTheRunnerLooksAwayOrCannotSeeThem() {
        session.start(0);

        runnerFaces(FACING_WEST);
        assertFalse(rules.isHeldByGaze(hunter));

        runnerFaces(FACING_EAST);
        clearView.set(false);
        assertFalse(rules.isHeldByGaze(hunter));
    }

    @Test
    void onlyAppliesWhileHuntingAndWhenTurnedOn() throws IOException {
        session.start(30);
        assertFalse(rules.isHeldByGaze(hunter));

        session.stop();
        session.reset();
        session.start(0);
        fixture.setConfig(ConfigKey.RULES_FREEZE_WHEN_LOOKED_AT, false);
        assertFalse(rules.isHeldByGaze(hunter));
    }

    @Test
    void eliminatedRunnersGazeDoesNotCount() {
        PlayerMock otherRunner = fixture.server().addPlayer("OtherRunner");
        session.assignRole(otherRunner.getUniqueId(), Role.RUNNER);
        session.start(0);

        session.eliminateRunner(runner.getUniqueId());

        assertFalse(rules.isHeldByGaze(hunter));
    }

    @Test
    void runnersAreNeverHeld() {
        session.start(0);
        hunter.teleport(new Location(world, 0, 64, 0, FACING_WEST, 0));
        runner.teleport(new Location(world, -10, 64, 0));

        assertFalse(rules.isHeldByGaze(runner));
    }

    @Test
    void heldHunterKeepsTheirPlaceButCanLookAround() {
        session.start(0);
        Location from = hunter.getLocation();
        Location to = from.clone().add(2, 0, 0);
        to.setYaw(45);
        PlayerMoveEvent move = new PlayerMoveEvent(hunter, from, to);

        new RulesListener(rules).onMove(move);

        assertEquals(from.getX(), move.getTo().getX());
        assertEquals(45, move.getTo().getYaw());
    }
}
