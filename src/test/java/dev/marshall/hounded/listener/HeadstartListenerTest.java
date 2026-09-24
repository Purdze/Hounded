package dev.marshall.hounded.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.testing.PluginFixture;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Pig;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.LivingEntitySimulation;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

/** The hunter is frozen through a real round with a long headstart. */
class HeadstartListenerTest {
    private static final int LONG_HEADSTART = 60;

    private PluginFixture fixture;
    private PlayerMock hunter;
    private PlayerMock otherHunter;
    private PlayerMock runner;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        hunter = fixture.addAdmin("Hunter");
        otherHunter = fixture.server().addPlayer("OtherHunter");
        runner = fixture.server().addPlayer("Runner");
        hunter.performCommand("hounded hunter add OtherHunter");
        fixture.startRound(hunter, LONG_HEADSTART, runner);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    /**
     * Where the server would put the player after trying to walk three blocks while turning east.
     * Read from the event, because MockBukkit moves the player before listeners can change the
     * destination.
     */
    private static Location destinationOfWalk(PlayerMock player) {
        Location target = player.getLocation().add(3, 0, 0);
        target.setYaw(-90);
        PlayerMoveEvent event = new PlayerSimulation(player).simulatePlayerMove(target);
        return event.isCancelled() ? event.getFrom() : event.getTo();
    }

    private static boolean walks(PlayerMock player) {
        double startX = player.getLocation().getX();
        return destinationOfWalk(player).getX() != startX;
    }

    private boolean isCancelled(Cancellable event) {
        fixture.server().getPluginManager().callEvent((Event) event);
        return event.isCancelled();
    }

    private boolean teleportIsCancelled(PlayerMock player, TeleportCause cause) {
        Location away = player.getLocation().add(100, 0, 0);
        return isCancelled(new PlayerTeleportEvent(player, player.getLocation(), away, cause));
    }

    private static DamageSource genericDamage() {
        return DamageSource.builder(DamageType.GENERIC).build();
    }

    @Test
    void hunterCannotWalkButCanLookAround() {
        double startX = hunter.getLocation().getX();

        Location destination = destinationOfWalk(hunter);

        assertEquals(startX, destination.getX());
        assertEquals(-90, destination.getYaw());
    }

    @Test
    void runnerCanWalk() {
        assertTrue(walks(runner));
    }

    @Test
    void hunterCannotBreakOrPlaceBlocks() {
        PlayerSimulation simulation = new PlayerSimulation(hunter);
        Block block = hunter.getLocation().add(1, 0, 0).getBlock();
        block.setType(Material.STONE);

        simulation.simulateBlockBreak(block);
        BlockPlaceEvent place = simulation.simulateBlockPlace(
                Material.DIRT, hunter.getLocation().add(0, 0, 2));

        assertEquals(Material.STONE, block.getType());
        assertTrue(place.isCancelled());
    }

    @Test
    void hunterCannotUseItems() {
        PlayerInteractEvent event =
                new PlayerInteractEvent(hunter, Action.RIGHT_CLICK_AIR, null, null, BlockFace.SELF, EquipmentSlot.HAND);

        fixture.server().getPluginManager().callEvent(event);

        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    void hunterCannotBeHurt() {
        double health = hunter.getHealth();

        EntityDamageEvent event = new LivingEntitySimulation(hunter).simulateDamage(5, genericDamage());

        assertTrue(event.isCancelled());
        assertEquals(health, hunter.getHealth());
    }

    @Test
    void hunterCannotHurtAnyone() {
        double health = runner.getHealth();

        EntityDamageEvent event = new LivingEntitySimulation(runner).simulateDamage(5, hunter);

        assertTrue(event.isCancelled());
        assertEquals(health, runner.getHealth());
    }

    @Test
    void hunterCannotUseOrMountEntities() {
        Pig pig = hunter.getWorld().spawn(hunter.getLocation(), Pig.class);
        ArmorStand stand = hunter.getWorld().spawn(hunter.getLocation(), ArmorStand.class);
        Boat boat = hunter.getWorld().spawn(hunter.getLocation(), Boat.class);

        assertTrue(isCancelled(new PlayerInteractEntityEvent(otherHunter, pig)));
        assertTrue(isCancelled(new PlayerInteractAtEntityEvent(otherHunter, stand, new Vector())));
        assertTrue(isCancelled(new EntityMountEvent(otherHunter, pig)));
        assertTrue(isCancelled(new VehicleEnterEvent(boat, otherHunter)));
        assertFalse(isCancelled(new EntityMountEvent(runner, pig)));
        assertFalse(isCancelled(new VehicleEnterEvent(boat, runner)));
    }

    @Test
    void hunterRidingWhenTheHeadstartStartsIsPutOnFoot() {
        hunter.performCommand("hounded stop");
        Pig pig = otherHunter.getWorld().spawn(otherHunter.getLocation(), Pig.class);
        pig.addPassenger(otherHunter);

        hunter.performCommand("hounded start " + LONG_HEADSTART);

        assertFalse(otherHunter.isInsideVehicle());
    }

    @Test
    void frozenHunterCannotBeTeleportedAwayUnlessAnAdmin() {
        assertTrue(teleportIsCancelled(otherHunter, TeleportCause.COMMAND));
        assertTrue(teleportIsCancelled(otherHunter, TeleportCause.PLUGIN));
        assertTrue(teleportIsCancelled(otherHunter, TeleportCause.ENDER_PEARL));
        assertFalse(teleportIsCancelled(otherHunter, TeleportCause.DISMOUNT));
        assertFalse(teleportIsCancelled(hunter, TeleportCause.COMMAND));
        assertFalse(teleportIsCancelled(runner, TeleportCause.COMMAND));
    }

    @Test
    void nothingIsBlockedOnceTheHeadstartIsOver() {
        hunter.performCommand("hounded stop");
        Pig pig = otherHunter.getWorld().spawn(otherHunter.getLocation(), Pig.class);

        assertFalse(teleportIsCancelled(otherHunter, TeleportCause.COMMAND));
        assertFalse(isCancelled(new EntityMountEvent(otherHunter, pig)));
    }

    @Test
    void hunterIsFreeOnceTheRoundIsStopped() {
        hunter.performCommand("hounded stop");

        assertTrue(walks(hunter));
    }

    @Test
    void hunterCanWalkWhenFreezingIsTurnedOff() throws IOException {
        fixture.setConfig(ConfigKey.HEADSTART_FREEZE_HUNTERS, false);
        hunter.performCommand("hounded reload");

        assertTrue(walks(hunter));
    }
}
