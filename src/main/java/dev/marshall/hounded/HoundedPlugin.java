package dev.marshall.hounded;

import dev.marshall.hounded.command.HoundedCommand;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.ConfigLoader;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.listener.RoundListener;
import dev.marshall.hounded.round.RoundService;
import dev.marshall.hounded.tracking.CompassItem;
import dev.marshall.hounded.tracking.TargetResolver;
import dev.marshall.hounded.tracking.TrackingService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Clock;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/** Bootstrap only: wires services together on enable and tears them down on disable. */
public final class HoundedPlugin extends JavaPlugin {
    // Null until onEnable succeeds; onDisable also runs after a failed enable.
    private RoundService roundService;
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
        roundService = new RoundService(this, session, configService);
        trackingService = new TrackingService(session, new TargetResolver(), new CompassItem(this));
        trackingService.start();

        getServer().getPluginManager().registerEvents(new RoundListener(roundService), this);
        HoundedCommand command = new HoundedCommand(session, roundService, configService, getServer());
        getLifecycleManager()
                .registerEventHandler(
                        LifecycleEvents.COMMANDS,
                        event -> event.registrar()
                                .register(command.build(), getPluginMeta().getDescription(), List.of()));
    }

    @Override
    public void onDisable() {
        if (roundService != null) {
            roundService.shutdown();
            roundService = null;
        }
        if (trackingService != null) {
            trackingService.stop();
            trackingService = null;
        }
    }
}
