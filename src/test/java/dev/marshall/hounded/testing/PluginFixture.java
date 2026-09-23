package dev.marshall.hounded.testing;

import dev.marshall.hounded.HoundedPlugin;
import dev.marshall.hounded.config.ConfigKey;
import dev.marshall.hounded.config.ConfigLoadException;
import dev.marshall.hounded.config.ConfigLoader;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * A mock server with Hounded enabled. Expected messages are rendered from the plugin's own
 * messages.yml, so tests don't depend on the English wording.
 */
public final class PluginFixture implements AutoCloseable {
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

    public boolean hasScheduledTasks() {
        return !server.getScheduler().getPendingTasks().isEmpty();
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

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Override
    public void close() {
        MockBukkit.unmock();
    }
}
