package dev.marshall.hounded;

import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.ConfigLoader;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.tracking.CompassItem;
import dev.marshall.hounded.tracking.TargetResolver;
import dev.marshall.hounded.tracking.TrackingService;
import java.time.Clock;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/** Bootstrap only: wires services together on enable and tears them down on disable. */
public final class HoundedPlugin extends JavaPlugin {
    // Null until onEnable succeeds; onDisable also runs after a failed enable.
    private TrackingService trackingService;

    @Override
    public void onEnable() {
        ConfigService configService;
        try {
            configService = new ConfigService(new ConfigLoader(this), getLogger());
        } catch (ConfigLoadException exception) {
            getLogger().log(Level.SEVERE, "Hounded could not load its config and will disable itself", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        GameSession session = new GameSession(Clock.systemUTC());
        trackingService = new TrackingService(session, new TargetResolver(), new CompassItem(this));
        trackingService.start();

        // TODO(scaffold): register commands (Brigadier via getLifecycleManager()) and listeners, and
        // pass configService to them.
        getLogger().info("Hounded enabled with " + configService.settings());
    }

    @Override
    public void onDisable() {
        if (trackingService != null) {
            trackingService.stop();
            trackingService = null;
        }
    }
}
