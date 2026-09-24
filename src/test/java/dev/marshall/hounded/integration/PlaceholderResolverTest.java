package dev.marshall.hounded.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.time.Duration;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Placeholder values on a session this test controls, with a hand-moved clock. */
class PlaceholderResolverTest {
    private final MutableClock clock = new MutableClock();
    private final GameSession session = new GameSession(clock);
    private PluginFixture fixture;
    private PlaceholderResolver placeholders;
    private PlayerMock hunter;
    private PlayerMock runner;
    private PlayerMock bystander;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        World world = fixture.addWorld("world", World.Environment.NORMAL);
        placeholders = fixture.placeholderResolverFor(session);
        hunter = fixture.server().addPlayer("Hunter");
        runner = fixture.server().addPlayer("Runner");
        bystander = fixture.server().addPlayer("Bystander");
        hunter.teleport(new Location(world, 0, 64, 0));
        runner.teleport(new Location(world, 30, 64, 40));
        session.assignRole(hunter.getUniqueId(), Role.HUNTER);
        session.assignRole(runner.getUniqueId(), Role.RUNNER);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private String valueFor(PlayerMock player, String name) {
        return placeholders.resolve(HoundedPlaceholder.byName(name).orElseThrow(), Optional.of(player));
    }

    @Test
    void roleIsTheTranslatedRoleNameOrEmpty() {
        assertEquals(fixture.text(MessageKey.ROLE_NAME_HUNTER), valueFor(hunter, "role"));
        assertEquals(fixture.text(MessageKey.ROLE_NAME_RUNNER), valueFor(runner, "role"));
        assertEquals("", valueFor(bystander, "role"));
    }

    @Test
    void stateFollowsTheRound() {
        assertEquals(fixture.text(MessageKey.STATE_LOBBY), valueFor(bystander, "state"));
        session.start(30);
        assertEquals(fixture.text(MessageKey.STATE_HEADSTART), valueFor(bystander, "state"));
        clock.advance(Duration.ofSeconds(30));
        session.tick();
        assertEquals(fixture.text(MessageKey.STATE_RUNNING), valueFor(bystander, "state"));
    }

    @Test
    void headstartCountsDownAndTimerCountsUp() {
        session.start(30);
        clock.advance(Duration.ofSeconds(10));
        assertEquals("0:20", valueFor(hunter, "headstart"));
        assertEquals("0:00", valueFor(hunter, "timer"));

        clock.advance(Duration.ofSeconds(20));
        session.tick();
        clock.advance(Duration.ofSeconds(75));
        assertEquals("0:00", valueFor(hunter, "headstart"));
        assertEquals("1:15", valueFor(hunter, "timer"));
    }

    @Test
    void huntersSeeDistanceAndTargetOthersSeeNothing() {
        session.start(0);

        assertEquals("50", valueFor(hunter, "distance"));
        assertEquals(runner.getName(), valueFor(hunter, "target"));
        assertEquals("", valueFor(runner, "distance"));
        assertEquals("", valueFor(bystander, "target"));
    }

    @Test
    void runnersLeftDropsWhenARunnerIsOut() {
        PlayerMock otherRunner = fixture.server().addPlayer("OtherRunner");
        session.assignRole(otherRunner.getUniqueId(), Role.RUNNER);
        session.start(0);
        assertEquals("2", valueFor(bystander, "runners_left"));

        session.eliminateRunner(runner.getUniqueId());

        assertEquals("1", valueFor(bystander, "runners_left"));
    }

    @Test
    void worksWithoutAPlayer() {
        assertEquals("", placeholders.resolve(HoundedPlaceholder.ROLE, Optional.empty()));
        assertEquals(
                fixture.text(MessageKey.STATE_LOBBY), placeholders.resolve(HoundedPlaceholder.STATE, Optional.empty()));
    }
}
