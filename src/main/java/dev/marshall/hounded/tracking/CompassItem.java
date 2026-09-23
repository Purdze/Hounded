package dev.marshall.hounded.tracking;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
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
     * Points the compass at {@code target}.
     *
     * <p>TODO(verify-on-26.2): implement with {@code CompassMeta#setLodestone} plus
     * {@code setLodestoneTracked(false)} (no real lodestone block). Before building on it, verify
     * on a Paper 26.2 server:
     *
     * <ol>
     *   <li>Does an untracked lodestone target point correctly in the Nether and the End, where a
     *       normal compass spins?
     *   <li>What does the needle do when the lodestone location's world differs from the holder's
     *       world? (TargetResolver should prevent that, but we need to know the failure mode.)
     *   <li>Does rewriting the meta every update interval replay the hotbar "re-equip" animation or
     *       make the item flicker? If so, only write when the target block actually changes.
     *   <li>Is the needle accurate underground/in caves when the target is far above or below?
     *   <li>Does the persistent-data marker survive death drops, respawn re-issue and /reload?
     *   <li>Is there a cleaner item-component API on 26.2 (lodestone tracker component) that avoids
     *       rebuilding the whole meta?
     * </ol>
     */
    public void pointAt(ItemStack compass, Location target) {
        Objects.requireNonNull(compass, "compass");
        Objects.requireNonNull(target, "target");
        // Intentionally a no-op until the questions above are answered on a real server.
    }
}
