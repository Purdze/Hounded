package dev.marshall.hounded.integration;

import io.papermc.paper.plugin.configuration.PluginMeta;

/**
 * The only way into PlaceholderAPI types. Call {@link #register} only when PlaceholderAPI is
 * enabled: loading {@link HoundedExpansion} without it on the server fails.
 */
public final class PlaceholderApiHook {
    public static final String PLUGIN_NAME = "PlaceholderAPI";

    private PlaceholderApiHook() {}

    /** @return undoes the registration, for plugin shutdown */
    public static Runnable register(PluginMeta pluginMeta, PlaceholderResolver resolver) {
        HoundedExpansion expansion = new HoundedExpansion(pluginMeta, resolver);
        expansion.register();
        return expansion::unregister;
    }
}
