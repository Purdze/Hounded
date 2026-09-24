package dev.marshall.hounded;

import dev.marshall.hounded.command.HoundedCommand;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.ConfigLoader;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.display.HudService;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.integration.PlaceholderApiHook;
import dev.marshall.hounded.integration.PlaceholderResolver;
import dev.marshall.hounded.listener.DisplayListener;
import dev.marshall.hounded.listener.HeadstartListener;
import dev.marshall.hounded.listener.OnboardingListener;
import dev.marshall.hounded.listener.RoundListener;
import dev.marshall.hounded.listener.RulesListener;
import dev.marshall.hounded.listener.TrackingListener;
import dev.marshall.hounded.onboarding.FirstRoundMarker;
import dev.marshall.hounded.onboarding.QuickStartGuide;
import dev.marshall.hounded.role.RoleService;
import dev.marshall.hounded.round.HeadstartHold;
import dev.marshall.hounded.round.RoundService;
import dev.marshall.hounded.rules.RuleEnforcer;
import dev.marshall.hounded.tracking.CompassHandout;
import dev.marshall.hounded.tracking.CompassItem;
import dev.marshall.hounded.tracking.TargetResolver;
import dev.marshall.hounded.tracking.TrackingService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Bootstrap only: wires services together on enable and tears them down on disable. */
// Not final: MockBukkit subclasses the main class in tests.
public class HoundedPlugin extends JavaPlugin {
    // Filled as services start, so onDisable only stops what actually started (also after a failed
    // enable), in reverse order.
    private final Deque<Runnable> shutdownSteps = new ArrayDeque<>();

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
        CompassItem compassItem = new CompassItem(this);
        TrackingService trackingService =
                new TrackingService(this, session, new TargetResolver(), compassItem, configService);
        trackingService.start();
        shutdownSteps.push(trackingService::stop);
        HudService hudService = new HudService(this, session, trackingService, configService);
        hudService.start();
        shutdownSteps.push(hudService::stop);
        CompassHandout compassHandout =
                new CompassHandout(session, trackingService, compassItem, configService, getServer());
        HeadstartHold headstartHold = new HeadstartHold(session, configService, getServer());
        FirstRoundMarker firstRoundMarker = new FirstRoundMarker(this);
        RoundService roundService =
                new RoundService(this, session, configService, headstartHold, compassHandout, firstRoundMarker);
        shutdownSteps.push(roundService::shutdown);

        List.of(
                        new RoundListener(roundService),
                        new HeadstartListener(headstartHold),
                        new DisplayListener(hudService),
                        new OnboardingListener(new QuickStartGuide(this, configService, firstRoundMarker)),
                        new TrackingListener(trackingService, compassHandout, compassItem),
                        new RulesListener(
                                new RuleEnforcer(session, configService, getServer(), Player::hasLineOfSight)))
                .forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
        HoundedCommand command = new HoundedCommand(
                new RoleService(session, getServer().getPluginManager()),
                roundService,
                compassHandout,
                configService,
                getServer());
        if (getServer().getPluginManager().isPluginEnabled(PlaceholderApiHook.PLUGIN_NAME)) {
            shutdownSteps.push(PlaceholderApiHook.register(
                    getPluginMeta(), new PlaceholderResolver(session, trackingService, configService, getServer())));
            getLogger().info("PlaceholderAPI found: registered the %hounded_...% placeholders");
        }
        getLifecycleManager()
                .registerEventHandler(
                        LifecycleEvents.COMMANDS,
                        event -> event.registrar()
                                .register(command.build(), getPluginMeta().getDescription(), List.of()));
    }

    @Override
    public void onDisable() {
        while (!shutdownSteps.isEmpty()) {
            shutdownSteps.pop().run();
        }
    }
}
