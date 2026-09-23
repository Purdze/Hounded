package dev.marshall.hounded.round;

import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.Settings;
import dev.marshall.hounded.game.GameSession;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Keeps hunters waiting during the headstart: frozen (enforced by the headstart listener) and,
 * if configured, blind. Blindness lasts exactly as long as the headstart, so a hunter who is
 * offline at release needs no cleanup.
 */
public final class HeadstartHold {
    private final GameSession session;
    private final ConfigService configService;
    private final Server server;
    private final Set<UUID> blinded = new HashSet<>();

    public HeadstartHold(GameSession session, ConfigService configService, Server server) {
        this.session = Objects.requireNonNull(session, "session");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.server = Objects.requireNonNull(server, "server");
    }

    public boolean isFrozen(Player player) {
        return configService.settings().headstart().freezeHunters() && session.isHeldInHeadstart(player.getUniqueId());
    }

    void holdOnlineHunters() {
        server.getOnlinePlayers().forEach(this::hold);
    }

    void hold(Player player) {
        if (!session.isHeldInHeadstart(player.getUniqueId())) {
            return;
        }
        Settings.Headstart settings = configService.settings().headstart();
        if (settings.freezeHunters()) {
            player.sendMessage(configService.messages().chat(MessageKey.START_FROZEN));
        }
        if (settings.blindHunters()) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS, Ticks.from(session.headstartRemaining()), 0, false, false, false));
            blinded.add(player.getUniqueId());
        }
    }

    void releaseAll() {
        for (UUID hunter : List.copyOf(blinded)) {
            Optional.ofNullable(server.getPlayer(hunter))
                    .ifPresent(player -> player.removePotionEffect(PotionEffectType.BLINDNESS));
        }
        blinded.clear();
    }
}
