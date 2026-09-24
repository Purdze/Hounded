package dev.marshall.hounded.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.testing.MutableClock;
import dev.marshall.hounded.testing.PluginFixture;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class PlaceholderSnapshotTest {
    private final GameSession session = new GameSession(new MutableClock());
    private PluginFixture fixture;
    private PlaceholderSnapshot snapshot;
    private PlayerMock hunter;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        World world = fixture.addWorld("world", World.Environment.NORMAL);
        hunter = fixture.server().addPlayer("Hunter");
        runner = fixture.server().addPlayer("Runner");
        hunter.teleport(new Location(world, 0, 64, 0));
        runner.teleport(new Location(world, 30, 64, 40));
        session.assignRole(hunter.getUniqueId(), Role.HUNTER);
        session.assignRole(runner.getUniqueId(), Role.RUNNER);
        snapshot = new PlaceholderSnapshot(fixture.placeholderResolverFor(session), fixture.server());
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private String valueFor(PlayerMock player, String name) {
        return snapshot.valueFor(player, name).orElseThrow();
    }

    @Test
    void valuesChangeOnlyWhenRefreshed() {
        snapshot.refresh();
        assertEquals(fixture.text(MessageKey.STATE_LOBBY), valueFor(hunter, "state"));

        session.start(0);
        assertEquals(fixture.text(MessageKey.STATE_LOBBY), valueFor(hunter, "state"));

        snapshot.refresh();
        assertEquals(fixture.text(MessageKey.STATE_RUNNING), valueFor(hunter, "state"));
    }

    @Test
    void eachPlayerGetsTheirOwnValues() {
        session.start(0);
        snapshot.refresh();

        assertEquals(fixture.text(MessageKey.ROLE_NAME_HUNTER), valueFor(hunter, "role"));
        assertEquals(fixture.text(MessageKey.ROLE_NAME_RUNNER), valueFor(runner, "role"));
        assertEquals("50", valueFor(hunter, "distance"));
        assertEquals("", valueFor(runner, "distance"));
    }

    @Test
    void playersMissingFromTheSnapshotGetOnlyTheSharedValues() {
        snapshot.refresh();
        PlayerMock latecomer = fixture.server().addPlayer("Latecomer");
        session.assignRole(latecomer.getUniqueId(), Role.HUNTER);

        assertEquals("", valueFor(latecomer, "role"));
        assertEquals("", snapshot.valueFor(null, "role").orElseThrow());
        assertEquals(fixture.text(MessageKey.STATE_LOBBY), valueFor(latecomer, "state"));
    }

    @Test
    void namesAreCaseInsensitiveAndUnknownNamesAreNotResolved() {
        snapshot.refresh();

        assertEquals(fixture.text(MessageKey.ROLE_NAME_HUNTER), valueFor(hunter, "ROLE"));
        assertEquals(Optional.empty(), snapshot.valueFor(hunter, "nonsense"));
    }

    @Test
    void anotherThreadReadsTheSnapshotRatherThanLiveState() throws ExecutionException, InterruptedException {
        snapshot.refresh();
        session.start(0);

        String fromOtherThread =
                CompletableFuture.supplyAsync(() -> valueFor(hunter, "state")).get();

        assertEquals(fixture.text(MessageKey.STATE_LOBBY), fromOtherThread);
    }
}
