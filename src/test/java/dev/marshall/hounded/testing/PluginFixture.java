package dev.marshall.hounded.testing;

import dev.marshall.hounded.HoundedPlugin;
import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.ConfigLoader;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.integration.PlaceholderResolver;
import dev.marshall.hounded.onboarding.FirstRoundMarker;
import dev.marshall.hounded.round.HeadstartHold;
import dev.marshall.hounded.round.RoundService;
import dev.marshall.hounded.tracking.CompassHandout;
import dev.marshall.hounded.tracking.CompassItem;
import dev.marshall.hounded.tracking.TargetResolver;
import dev.marshall.hounded.tracking.TrackingService;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

/**
 * A mock server with Hounded enabled. Expected messages are rendered from the plugin's own
 * messages.yml, so tests don't depend on the English wording.
 */
public final class PluginFixture implements AutoCloseable {
    /** Rounds in tests last milliseconds, and the hunt timer shows whole seconds. */
    public static final String INSTANT_HUNT_TIME = "0:00";

    private final ServerMock server;
    private final HoundedPlugin plugin;
    private final ConfigService config;

    private PluginFixture(ServerMock server, HoundedPlugin plugin, ConfigService config) {
        this.server = server;
        this.plugin = plugin;
        this.config = config;
    }

    public static PluginFixture start() throws ConfigLoadException {
        ServerMock server = MockBukkit.mock();
        HoundedPlugin plugin = MockBukkit.load(HoundedPlugin.class);
        return new PluginFixture(server, plugin, new ConfigService(new ConfigLoader(plugin), plugin.getLogger()));
    }

    public ServerMock server() {
        return server;
    }

    public HoundedPlugin plugin() {
        return plugin;
    }

    public ConfigService config() {
        return config;
    }

    /** The compass services wired to {@code session}, for tests that drive their own round. */
    public record Tracking(CompassItem compassItem, TrackingService service, CompassHandout handout) {}

    public Tracking trackingFor(GameSession session) {
        CompassItem compassItem = new CompassItem(plugin);
        TrackingService service = new TrackingService(plugin, session, new TargetResolver(), compassItem, config);
        return new Tracking(compassItem, service, new CompassHandout(session, service, compassItem, config, server));
    }

    /** Placeholder values for {@code session}, with its own tracking. */
    public PlaceholderResolver placeholderResolverFor(GameSession session) {
        return new PlaceholderResolver(session, trackingFor(session).service(), config, server);
    }

    /** A round service on {@code session}, with its own tracking and headstart hold. */
    public RoundService roundServiceFor(GameSession session) {
        return new RoundService(
                plugin,
                session,
                config,
                new HeadstartHold(session, config, server),
                trackingFor(session).handout(),
                new FirstRoundMarker(plugin));
    }

    public PlayerMock addAdmin(String name) {
        PlayerMock admin = server.addPlayer(name);
        admin.setOp(true);
        return admin;
    }

    /** Makes {@code admin} a hunter and the others runners, through the real commands. */
    public void assignRoles(PlayerMock admin, PlayerMock... runners) {
        for (PlayerMock runner : runners) {
            admin.performCommand("hounded runner add " + runner.getName());
        }
        admin.performCommand("hounded hunter add " + admin.getName());
    }

    /** Assigns roles and starts a round, then clears everyone's chat. */
    public void startRound(PlayerMock admin, int headstartSeconds, PlayerMock... runners) {
        assignRoles(admin, runners);
        admin.performCommand("hounded start " + headstartSeconds);
        server.getOnlinePlayers().forEach(player -> messagesOf((PlayerMock) player));
    }

    /** Changes one value in the plugin's config.yml and reloads {@link #config()}. */
    public void setConfig(ConfigKey key, Object value) throws IOException {
        File file = new File(plugin.getDataFolder(), ConfigLoader.CONFIG_FILE);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set(key.path(), value);
        yaml.save(file);
        if (!config.reload()) {
            throw new IllegalStateException("Reloading the edited config.yml failed");
        }
    }

    /** Mock servers start without worlds; the first one added is where new players spawn. */
    public World addWorld(String name, World.Environment environment) {
        WorldMock world = server.addSimpleWorld(name);
        world.setEnvironment(environment);
        return world;
    }

    /** Where the first compass in the player's inventory points; empty if it has no target. */
    public static Optional<Location> compassTarget(PlayerMock player) {
        ItemStack compass = Arrays.stream(player.getInventory().getContents())
                .filter(item -> item != null && item.getType() == Material.COMPASS)
                .findFirst()
                .orElseThrow(() -> new AssertionError(player.getName() + " has no compass"));
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        return meta.hasLodestone() ? Optional.of(meta.getLodestone()) : Optional.empty();
    }

    /** Runs {@code handler} for every {@code type} event, as a real listener would. */
    public <T extends Event> void onEvent(Class<T> type, EventPriority priority, Consumer<T> handler) {
        server.getPluginManager()
                .registerEvent(
                        type,
                        new Listener() {},
                        priority,
                        (listener, event) -> {
                            if (type.isInstance(event)) {
                                handler.accept(type.cast(event));
                            }
                        },
                        plugin);
    }

    /** Every {@code type} event fired from now on, in order, as seen by the last listener. */
    public <T extends Event> List<T> captureEvents(Class<T> type) {
        List<T> captured = new ArrayList<>();
        onEvent(type, EventPriority.MONITOR, captured::add);
        return captured;
    }

    /** Makes another plugin cancel every {@code type} event from now on. */
    public <T extends Event & Cancellable> void cancelEvents(Class<T> type) {
        onEvent(type, EventPriority.NORMAL, event -> event.setCancelled(true));
    }

    public int scheduledTaskCount() {
        return server.getScheduler().getPendingTasks().size();
    }

    /** Changes a config value and applies it through the real {@code /hounded reload}. */
    public void reloadWith(PlayerMock admin, ConfigKey key, Object value) throws IOException {
        setConfig(key, value);
        admin.performCommand("hounded reload");
        messagesOf(admin);
    }

    /** A message without the chat prefix, as plain text. */
    public String text(MessageKey key, TagResolver... placeholders) {
        return plain(config.messages().render(key, placeholders));
    }

    /** The chat line a player would see for {@code key}, as plain text. */
    public String chat(MessageKey key, TagResolver... placeholders) {
        return plain(config.messages().chat(key, placeholders));
    }

    /** A message whose {@code <player>} is {@code player}, plus any other placeholders. */
    public String aboutPlayer(MessageKey key, PlayerMock player, TagResolver... placeholders) {
        return chat(
                key,
                Placeholder.unparsed(PlaceholderNames.PLAYER, player.getName()),
                TagResolver.resolver(placeholders));
    }

    /** The "left, has N seconds to come back" broadcast with the configured grace. */
    public String runnerLeftMessage(PlayerMock runner) {
        return aboutPlayer(
                MessageKey.ROUND_RUNNER_LEFT,
                runner,
                Placeholder.unparsed(
                        PlaceholderNames.SECONDS,
                        Integer.toString(config.settings().rules().runnerRejoinGraceSeconds())));
    }

    public String winMessage(MessageKey key, String huntTime) {
        return chat(key, Placeholder.unparsed(PlaceholderNames.TIME, huntTime));
    }

    /** Runs a command as {@code sender} and returns only the replies it caused. */
    public static List<String> run(PlayerMock sender, String command) {
        messagesOf(sender);
        sender.performCommand(command);
        return messagesOf(sender);
    }

    /** Every message the player received since the last call, as plain text. */
    public static List<String> messagesOf(PlayerMock player) {
        List<String> messages = new ArrayList<>();
        for (Component message = player.nextComponentMessage();
                message != null;
                message = player.nextComponentMessage()) {
            messages.add(plain(message));
        }
        return messages;
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Override
    public void close() {
        MockBukkit.unmock();
    }
}
