package dev.marshall.hounded.listener;

import static dev.marshall.hounded.testing.PluginFixture.compassTarget;
import static dev.marshall.hounded.testing.PluginFixture.messagesOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import dev.marshall.hounded.config.CompassUpdateMode;
import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.testing.PluginFixture;
import dev.marshall.hounded.tracking.CompassItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** The compass through a real round: handed out, kept, switched, updated and collected. */
class CompassLifecycleTest {
    private static final long UPDATE_INTERVAL_TICKS = 20;

    private PluginFixture fixture;
    private CompassItem compassItem;
    private World overworld;
    private World nether;
    private PlayerMock hunter;
    private PlayerMock runner;
    private PlayerMock otherRunner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        compassItem = new CompassItem(fixture.plugin());
        overworld = fixture.addWorld("world", World.Environment.NORMAL);
        nether = fixture.addWorld("world_nether", World.Environment.NETHER);
        hunter = fixture.addAdmin("Hunter");
        runner = fixture.server().addPlayer("Runner");
        otherRunner = fixture.server().addPlayer("OtherRunner");
        runner.teleport(new Location(overworld, 100, 64, 0));
        otherRunner.teleport(new Location(overworld, -100, 64, 0));
        fixture.startRound(hunter, 0, runner, otherRunner);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private ItemStack compassOf(PlayerMock player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (compassItem.isTrackingCompass(item)) {
                return item;
            }
        }
        throw new AssertionError(player.getName() + " has no tracking compass");
    }

    private boolean carriesCompass(PlayerMock player) {
        return compassItem.isIn(player.getInventory());
    }

    private void click(Action action) {
        fixture.server()
                .getPluginManager()
                .callEvent(new PlayerInteractEvent(
                        hunter, action, compassOf(hunter), null, BlockFace.SELF, EquipmentSlot.HAND));
    }

    private String aboutRunner(MessageKey key, PlayerMock tracked) {
        return fixture.chat(key, Placeholder.unparsed(PlaceholderNames.RUNNER, tracked.getName()));
    }

    @Test
    void onlyHuntersGetACompassWhenTheRoundStarts() {
        assertEquals(Optional.of(new Location(overworld, 100, 64, 0)), compassTarget(hunter));
        assertFalse(carriesCompass(runner));
    }

    @Test
    void compassCannotBeDropped() {
        Item dropped = overworld.dropItem(hunter.getLocation(), compassOf(hunter));
        PlayerDropItemEvent event = new PlayerDropItemEvent(hunter, dropped);

        fixture.server().getPluginManager().callEvent(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void compassStaysOutOfDeathDropsAndComesBackOnRespawn() {
        hunter.getInventory().addItem(ItemStack.of(Material.STONE));
        List<ItemStack> drops = new ArrayList<>();
        fixture.server()
                .getPluginManager()
                .registerEvents(
                        new Listener() {
                            @EventHandler(priority = EventPriority.MONITOR)
                            public void capture(PlayerDeathEvent event) {
                                drops.addAll(event.getDrops());
                            }
                        },
                        fixture.plugin());

        hunter.setHealth(0);
        hunter.getInventory().clear();
        fixture.server()
                .getPluginManager()
                .callEvent(new PlayerPostRespawnEvent(
                        hunter, hunter.getLocation(), false, false, false, PlayerRespawnEvent.RespawnReason.DEATH));

        assertTrue(drops.stream().anyMatch(item -> item.getType() == Material.STONE));
        assertTrue(drops.stream().noneMatch(compassItem::isTrackingCompass));
        assertTrue(carriesCompass(hunter));
    }

    @Test
    void roundEndCollectsTheCompass() {
        hunter.performCommand("hounded stop");

        assertFalse(carriesCompass(hunter));
    }

    @Test
    void leftoverCompassFromAnEarlierRoundIsTakenOnJoin() {
        hunter.performCommand("hounded stop");
        hunter.getInventory().addItem(compassItem.create(Component.text("Old")));

        hunter.disconnect();
        hunter.reconnect();

        assertFalse(carriesCompass(hunter));
    }

    @Test
    void hunterRejoiningMidRoundGetsACompassBack() {
        hunter.getInventory().clear();

        hunter.disconnect();
        hunter.reconnect();

        assertTrue(carriesCompass(hunter));
    }

    @Test
    void leftClickSwitchesToTheNextRunner() {
        messagesOf(hunter);

        click(Action.LEFT_CLICK_AIR);

        assertEquals(List.of(aboutRunner(MessageKey.COMPASS_NOW_TRACKING, otherRunner)), messagesOf(hunter));
        assertEquals(Optional.of(new Location(overworld, -100, 64, 0)), compassTarget(hunter));
    }

    @Test
    void autoModeFollowsTheRunnerOnItsOwn() {
        runner.teleport(new Location(overworld, 150, 64, 30));

        fixture.server().getScheduler().performTicks(UPDATE_INTERVAL_TICKS);

        assertEquals(Optional.of(new Location(overworld, 150, 64, 30)), compassTarget(hunter));
    }

    @Test
    void manualModeWaitsForARightClick() throws IOException {
        fixture.reloadWith(
                hunter,
                ConfigKey.COMPASS_UPDATE_MODE,
                CompassUpdateMode.MANUAL.name().toLowerCase());
        runner.teleport(new Location(overworld, 150, 64, 30));

        fixture.server().getScheduler().performTicks(UPDATE_INTERVAL_TICKS * 3);
        assertEquals(Optional.of(new Location(overworld, 100, 64, 0)), compassTarget(hunter));

        click(Action.RIGHT_CLICK_AIR);
        assertEquals(Optional.of(new Location(overworld, 150, 64, 30)), compassTarget(hunter));
    }

    @Test
    void compassCanBeTurnedOffInTheNether() throws IOException {
        fixture.reloadWith(hunter, ConfigKey.COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS, true);
        hunter.teleport(new Location(nether, 0, 64, 0));

        fixture.server().getScheduler().performTicks(UPDATE_INTERVAL_TICKS * 2);

        assertEquals(Optional.empty(), compassTarget(hunter));
        assertEquals(List.of(aboutRunner(MessageKey.COMPASS_DISABLED_IN_NETHER, runner)), messagesOf(hunter));
    }
}
