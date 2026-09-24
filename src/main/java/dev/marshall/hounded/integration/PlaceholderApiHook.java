package dev.marshall.hounded.integration;

import dev.marshall.hounded.Ticks;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * The only way into PlaceholderAPI types. Call {@link #register} only when PlaceholderAPI is
 * enabled: loading {@link HoundedExpansion} without it on the server fails.
 */
public final class PlaceholderApiHook {
    public static final String PLUGIN_NAME = "PlaceholderAPI";

    private PlaceholderApiHook() {}

    /** @return undoes the registration and stops the refreshes, for plugin shutdown */
    public static Runnable register(Plugin plugin, PlaceholderResolver resolver) {
        PlaceholderSnapshot snapshot = new PlaceholderSnapshot(resolver, plugin.getServer());
        snapshot.refresh();
        BukkitTask refreshTask = plugin.getServer()
                .getScheduler()
                .runTaskTimer(plugin, snapshot::refresh, Ticks.PER_SECOND, Ticks.PER_SECOND);
        HoundedExpansion expansion = new HoundedExpansion(plugin.getPluginMeta(), snapshot);
        expansion.register();
        return () -> {
            expansion.unregister();
            refreshTask.cancel();
        };
    }
}
