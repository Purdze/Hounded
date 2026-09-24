package dev.marshall.hounded.integration;

import dev.marshall.hounded.PlayerNames;
import dev.marshall.hounded.config.ConfigService;
import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.config.RoleMessages;
import dev.marshall.hounded.game.GameSession;
import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.round.HuntTimeFormatter;
import dev.marshall.hounded.tracking.TrackingReading;
import dev.marshall.hounded.tracking.TrackingService;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

/**
 * Works out placeholder values without depending on PlaceholderAPI, so it can be tested on its own.
 * Values are plain text: other plugins expect strings, not formatted components. A placeholder that
 * doesn't apply to the player is an empty string.
 */
public final class PlaceholderResolver {
    private static final String NOT_APPLICABLE = "";

    private final GameSession session;
    private final TrackingService trackingService;
    private final ConfigService configService;
    private final Server server;

    public PlaceholderResolver(
            GameSession session, TrackingService trackingService, ConfigService configService, Server server) {
        this.session = Objects.requireNonNull(session, "session");
        this.trackingService = Objects.requireNonNull(trackingService, "trackingService");
        this.configService = Objects.requireNonNull(configService, "configService");
        this.server = Objects.requireNonNull(server, "server");
    }

    /**
     * @param player who the placeholder is for; may be null when there is no player context
     * @return empty for a name Hounded doesn't know
     */
    public Optional<String> resolve(OfflinePlayer player, String name) {
        Optional<OfflinePlayer> forPlayer = Optional.ofNullable(player);
        return HoundedPlaceholder.byName(name).map(placeholder -> valueOf(placeholder, forPlayer));
    }

    private String valueOf(HoundedPlaceholder placeholder, Optional<OfflinePlayer> player) {
        return switch (placeholder) {
            case ROLE ->
                player.flatMap(known -> session.roleOf(known.getUniqueId()))
                        .map(role -> text(RoleMessages.singular(role)))
                        .orElse(NOT_APPLICABLE);
            case STATE -> text(stateName(session.state()));
            case TIMER -> HuntTimeFormatter.format(session.elapsedHuntTime());
            case HEADSTART -> HuntTimeFormatter.format(session.headstartRemaining());
            case DISTANCE ->
                reading(player)
                        .map(TrackingReading::distance)
                        .filter(OptionalInt::isPresent)
                        .map(distance -> Integer.toString(distance.getAsInt()))
                        .orElse(NOT_APPLICABLE);
            case TARGET ->
                reading(player)
                        .map(reading -> PlayerNames.displayName(server, reading.runner()))
                        .orElse(NOT_APPLICABLE);
            case RUNNERS_LEFT -> Integer.toString(session.remainingRunners().size());
        };
    }

    /** Only an online hunter during the hunt has a compass reading. */
    private Optional<TrackingReading> reading(Optional<OfflinePlayer> player) {
        return player.map(OfflinePlayer::getPlayer).flatMap(trackingService::read);
    }

    private static MessageKey stateName(GameState state) {
        return switch (state) {
            case LOBBY -> MessageKey.STATE_LOBBY;
            case HEADSTART -> MessageKey.STATE_HEADSTART;
            case RUNNING -> MessageKey.STATE_RUNNING;
            case ENDED -> MessageKey.STATE_ENDED;
        };
    }

    private String text(MessageKey key) {
        return PlainTextComponentSerializer.plainText()
                .serialize(configService.messages().render(key));
    }
}
