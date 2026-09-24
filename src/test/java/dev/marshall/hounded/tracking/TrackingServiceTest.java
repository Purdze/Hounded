package dev.marshall.hounded.tracking;

import static dev.marshall.hounded.testing.PluginFixture.compassTarget;
import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Builds its own service and session, so the round is controlled directly. */
class TrackingServiceTest {
    private final GameSession session = new GameSession(new MutableClock());
    private PluginFixture fixture;
    private CompassItem compassItem;
    private TrackingService tracking;
    private CompassHandout handout;
    private World overworld;
    private World nether;
    private World end;
    private PlayerMock hunter;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        PluginFixture.Tracking parts = fixture.trackingFor(session);
        compassItem = parts.compassItem();
        tracking = parts.service();
        handout = parts.handout();
        overworld = fixture.addWorld("world", World.Environment.NORMAL);
        nether = fixture.addWorld("world_nether", World.Environment.NETHER);
        end = fixture.addWorld("world_the_end", World.Environment.THE_END);
        hunter = fixture.server().addPlayer("Hunter");
        runner = fixture.server().addPlayer("Runner");
        session.assignRole(hunter.getUniqueId(), Role.HUNTER);
        session.assignRole(runner.getUniqueId(), Role.RUNNER);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private static Location block(World world, int x, int y, int z) {
        return new Location(world, x, y, z);
    }

    /** The runner walks from {@code exit} through a portal to {@code arrival}. */
    private void runnerTakesPortal(Location exit, Location arrival) {
        runner.teleport(exit);
        tracking.recordWorldChange(runner, exit);
        runner.teleport(arrival);
    }

    @Test
    void sameDimensionPointsAtTheRunner() {
        session.start(0);
        runner.teleport(block(overworld, 100, 64, -50));

        handout.give(hunter);

        assertEquals(Optional.of(block(overworld, 100, 64, -50)), compassTarget(hunter));
    }

    @Test
    void runnerInAnotherDimensionIsTrackedThroughTheirPortal() {
        session.start(0);
        runnerTakesPortal(block(overworld, 10, 70, 20), block(nether, 1, 64, 2));

        handout.give(hunter);

        assertEquals(Optional.of(block(overworld, 10, 70, 20)), compassTarget(hunter));
    }

    @Test
    void hunterFollowingIntoTheNetherPointsAtTheRunnerThere() {
        session.start(0);
        runnerTakesPortal(block(overworld, 10, 70, 20), block(nether, 1, 64, 2));
        handout.give(hunter);

        hunter.teleport(block(nether, 50, 64, 50));
        tracking.updateCompass(hunter, false);

        assertEquals(Optional.of(block(nether, 1, 64, 2)), compassTarget(hunter));
    }

    @Test
    void withoutAnyTrailTheCompassPointsAtSpawnAndSaysSoOnce() {
        session.start(0);
        runnerTakesPortal(block(overworld, 10, 70, 20), block(nether, 1, 64, 2));
        hunter.teleport(block(end, 0, 64, 0));
        String noData = fixture.chat(
                MessageKey.COMPASS_NO_DATA, Placeholder.unparsed(PlaceholderNames.RUNNER, runner.getName()));

        handout.give(hunter);
        assertEquals(Optional.of(end.getSpawnLocation().toBlockLocation()), compassTarget(hunter));
        assertTrue(messagesOf(hunter).contains(noData));

        tracking.updateCompass(hunter, false);
        assertEquals(List.of(), messagesOf(hunter));

        tracking.updateCompass(hunter, true);
        assertEquals(List.of(noData), messagesOf(hunter));
    }

    @Test
    void offlineRunnerIsTrackedWhereTheyWereLastSeen() {
        session.start(0);
        runner.teleport(block(overworld, -30, 64, 7));
        tracking.recordQuit(runner);
        runner.disconnect();

        handout.give(hunter);

        assertEquals(Optional.of(block(overworld, -30, 64, 7)), compassTarget(hunter));
    }

    @Test
    void compassGivesNothingAwayDuringTheHeadstart() {
        session.start(30);
        runner.teleport(block(overworld, 100, 64, -50));

        assertTrue(handout.give(hunter));

        assertEquals(Optional.empty(), compassTarget(hunter));
    }

    @Test
    void onlyHuntersInARoundGetACompass() {
        assertFalse(handout.give(hunter));

        session.start(0);

        assertFalse(handout.give(runner));
        assertTrue(Arrays.stream(runner.getInventory().getContents())
                .filter(Objects::nonNull)
                .noneMatch(compassItem::isTrackingCompass));
    }

    @Test
    void readingInTheSameDimensionGivesTheDistanceToTheRunner() {
        session.start(0);
        hunter.teleport(block(overworld, 0, 64, 0));
        runner.teleport(block(overworld, 30, 90, 40));

        assertEquals(
                Optional.of(new TrackingReading(
                        runner.getUniqueId(),
                        TrackingReading.Kind.RUNNER,
                        Optional.of(Dimension.OVERWORLD),
                        OptionalInt.of(50))),
                tracking.read(hunter));
    }

    @Test
    void readingAcrossDimensionsGivesThePortalDistanceAndWhereTheRunnerIs() {
        session.start(0);
        hunter.teleport(block(overworld, 0, 64, 0));
        runnerTakesPortal(block(overworld, 0, 64, 12), block(nether, 1, 64, 2));

        assertEquals(
                Optional.of(new TrackingReading(
                        runner.getUniqueId(),
                        TrackingReading.Kind.PORTAL,
                        Optional.of(Dimension.NETHER),
                        OptionalInt.of(12))),
                tracking.read(hunter));
    }

    @Test
    void readingWithoutATrailHasNoDistance() {
        session.start(0);
        runnerTakesPortal(block(overworld, 0, 64, 12), block(nether, 1, 64, 2));
        hunter.teleport(block(end, 0, 64, 0));

        assertEquals(
                TrackingReading.Kind.NO_DATA,
                tracking.read(hunter).orElseThrow().kind());
        assertEquals(OptionalInt.empty(), tracking.read(hunter).orElseThrow().distance());
    }

    @Test
    void readingIsDisabledInTheNetherWhenConfigured() throws IOException {
        fixture.setConfig(ConfigKey.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS, true);
        session.start(0);
        hunter.teleport(block(nether, 0, 64, 0));

        assertEquals(
                TrackingReading.Kind.DISABLED,
                tracking.read(hunter).orElseThrow().kind());
    }

    @Test
    void nothingIsReadDuringTheHeadstart() {
        session.start(30);

        assertEquals(Optional.empty(), tracking.read(hunter));
    }
}
