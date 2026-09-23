package dev.marshall.hounded.round;

import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.PlaceholderNames;
import dev.marshall.hounded.game.GameOutcome;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.TransitionResult;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Turns {@link GameSession} results into server effects: broadcasts, the headstart timer and the
 * reset after a round ends. Commands and listeners go through here so they stay thin. Main thread
 * only.
 */
public final class RoundService {
    private static final long TICKS_PER_SECOND = 20L;

    private final Plugin plugin;
    private final GameSession session;
    private final ConfigService configService;
    private BukkitTask headstartTask;

    public RoundService(Plugin plugin, GameSession session, ConfigService configService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.session = Objects.requireNonNull(session, "session");
        this.configService = Objects.requireNonNull(configService, "configService");
    }

    public TransitionResult start(int headstartSeconds) {
        TransitionResult result = session.start(headstartSeconds);
        if (result instanceof TransitionResult.Changed changed) {
            if (changed.to() == GameState.HEADSTART) {
                broadcast(
                        MessageKey.START_HEADSTART,
                        Placeholder.unparsed(PlaceholderNames.SECONDS, Integer.toString(headstartSeconds)));
                headstartTask = plugin.getServer()
                        .getScheduler()
                        .runTaskTimer(plugin, this::tickHeadstart, TICKS_PER_SECOND, TICKS_PER_SECOND);
            } else {
                broadcast(MessageKey.START_RELEASED);
            }
        }
        return result;
    }

    public TransitionResult stop() {
        return finishIfEnded(session.stop());
    }

    public TransitionResult recordRunnerDeath(UUID player) {
        return finishIfEnded(session.recordRunnerDeath(player));
    }

    public TransitionResult recordDragonKilled() {
        return finishIfEnded(session.recordDragonKilled());
    }

    public void shutdown() {
        cancelHeadstartTask();
    }

    private void tickHeadstart() {
        if (session.tick() instanceof TransitionResult.Changed) {
            cancelHeadstartTask();
            broadcast(MessageKey.START_RELEASED);
        }
    }

    private TransitionResult finishIfEnded(TransitionResult result) {
        if (session.state() == GameState.ENDED) {
            cancelHeadstartTask();
            announce(session.outcome().orElseThrow());
            session.reset();
        }
        return result;
    }

    private void announce(GameOutcome outcome) {
        MessageKey key = switch (outcome) {
            case RUNNERS_WIN -> MessageKey.WIN_RUNNERS;
            case HUNTERS_WIN -> MessageKey.WIN_HUNTERS;
            case STOPPED -> MessageKey.STOP_STOPPED;
        };
        broadcast(
                key, Placeholder.unparsed(PlaceholderNames.TIME, HuntTimeFormatter.format(session.elapsedHuntTime())));
    }

    private void cancelHeadstartTask() {
        if (headstartTask != null) {
            headstartTask.cancel();
            headstartTask = null;
        }
    }

    private void broadcast(MessageKey key, TagResolver... placeholders) {
        plugin.getServer().broadcast(configService.messages().chat(key, placeholders));
    }
}
