package dev.marshall.hounded.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.testing.PluginFixture;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompassItemTest {
    private PluginFixture fixture;
    private CompassItem compassItem;
    private World world;

    @BeforeEach
    void setUp() throws ConfigLoadException {
        fixture = PluginFixture.start();
        compassItem = new CompassItem(fixture.plugin());
        world = fixture.addWorld("world", World.Environment.NORMAL);
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void recognisesOnlyItsOwnCompass() {
        assertTrue(compassItem.isTrackingCompass(compassItem.create(Component.text("Tracker"))));
        assertFalse(compassItem.isTrackingCompass(ItemStack.of(Material.COMPASS)));
        assertFalse(compassItem.isTrackingCompass(null));
    }

    @Test
    void pointsAtABlockWithoutNeedingALodestone() {
        ItemStack compass = compassItem.create(Component.text("Tracker"));

        assertTrue(compassItem.pointAt(compass, new Location(world, 5.7, 64.2, -3.4)));

        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        assertEquals(new Location(world, 5, 64, -4), meta.getLodestone());
        assertFalse(meta.isLodestoneTracked());
    }

    @Test
    void pointingAtTheSameBlockAgainChangesNothing() {
        ItemStack compass = compassItem.create(Component.text("Tracker"));
        compassItem.pointAt(compass, new Location(world, 5.1, 64, 3.1));

        assertFalse(compassItem.pointAt(compass, new Location(world, 5.9, 64.9, 3.9)));
        assertTrue(compassItem.pointAt(compass, new Location(world, 6, 64, 3)));
    }

    @Test
    void clearTargetReportsWhetherThereWasOne() {
        ItemStack compass = compassItem.create(Component.text("Tracker"));
        assertFalse(compassItem.clearTarget(compass));

        compassItem.pointAt(compass, new Location(world, 1, 2, 3));

        assertTrue(compassItem.clearTarget(compass));
        assertFalse(((CompassMeta) compass.getItemMeta()).hasLodestone());
    }
}
