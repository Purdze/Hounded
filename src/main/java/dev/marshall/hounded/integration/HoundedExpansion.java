package dev.marshall.hounded.integration;

import io.papermc.paper.plugin.configuration.PluginMeta;
import java.util.Objects;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** Hounded's {@code %hounded_...%} placeholders for PlaceholderAPI. Loaded only through {@link PlaceholderApiHook}. */
public final class HoundedExpansion extends PlaceholderExpansion {
    static final String IDENTIFIER = "hounded";

    private final PluginMeta pluginMeta;
    private final PlaceholderSnapshot snapshot;

    public HoundedExpansion(PluginMeta pluginMeta, PlaceholderSnapshot snapshot) {
        this.pluginMeta = Objects.requireNonNull(pluginMeta, "pluginMeta");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public String getAuthor() {
        return String.join(", ", pluginMeta.getAuthors());
    }

    @Override
    public String getVersion() {
        return pluginMeta.getVersion();
    }

    // Bundled with the plugin, so it must survive PlaceholderAPI's own reloads.
    @Override
    public boolean persist() {
        return true;
    }

    // PlaceholderAPI's contract: null means "not one of mine".
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return snapshot.valueFor(player, params).orElse(null);
    }
}
