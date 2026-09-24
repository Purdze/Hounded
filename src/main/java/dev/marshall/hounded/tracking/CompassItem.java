package dev.marshall.hounded.tracking;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Creates and recognises the hunter's tracking compass.
 *
 * <p>Our compass is marked with a persistent-data tag so listeners can tell it apart from a normal
 * compass (drop prevention, re-issue on respawn, click handling).
 */
public final class CompassItem {
    private static final String MARKER_KEY = "tracking_compass";

    private final NamespacedKey markerKey;

    public CompassItem(Plugin plugin) {
        this.markerKey = new NamespacedKey(Objects.requireNonNull(plugin, "plugin"), MARKER_KEY);
    }

    /** @param displayName already rendered from messages.yml, so the name is translatable */
    public ItemStack create(Component displayName) {
        ItemStack compass = ItemStack.of(Material.COMPASS);
        compass.editMeta(meta -> {
            meta.displayName(displayName);
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BOOLEAN, true);
        });
        return compass;
    }

    public boolean isTrackingCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BOOLEAN);
    }

    /**
     * Points the compass at {@code target} without needing a real lodestone block there.
     *
     * @return false if it already pointed at that block, so callers can skip re-sending the item
     */
    public boolean pointAt(ItemStack compass, Location target) {
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (meta.hasLodestone() && isSameBlock(meta.getLodestone(), target)) {
            return false;
        }
        meta.setLodestone(target.toBlockLocation());
        meta.setLodestoneTracked(false);
        compass.setItemMeta(meta);
        return true;
    }

    /** @return false if the compass had no target */
    public boolean clearTarget(ItemStack compass) {
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        if (!meta.hasLodestone()) {
            return false;
        }
        // Equivalent to clearLodestone(), which MockBukkit doesn't simulate; null is documented as "clear".
        meta.setLodestone(null);
        compass.setItemMeta(meta);
        return true;
    }

    /**
     * Applies {@code change} to every tracking compass in the inventory. Only slots that changed are
     * written back, so unchanged items aren't re-sent to the client.
     *
     * @param change modifies a compass and returns whether it did
     */
    public void updateAll(Inventory inventory, Predicate<ItemStack> change) {
        for (int slot : trackingCompassSlots(inventory)) {
            ItemStack compass = inventory.getItem(slot);
            if (change.test(compass)) {
                inventory.setItem(slot, compass);
            }
        }
    }

    public void removeAll(Inventory inventory) {
        for (int slot : trackingCompassSlots(inventory)) {
            inventory.setItem(slot, null);
        }
    }

    public boolean isIn(Inventory inventory) {
        return trackingCompassSlots(inventory).length > 0;
    }

    private int[] trackingCompassSlots(Inventory inventory) {
        return IntStream.range(0, inventory.getSize())
                .filter(slot -> isTrackingCompass(inventory.getItem(slot)))
                .toArray();
    }

    private static boolean isSameBlock(Location first, Location second) {
        return Objects.equals(first.getWorld(), second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }
}
