package dev.marshall.hounded.api;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a round ends, however it ends. Listen to this class to hear about
 * every end, or to {@link HoundedRoundWinEvent} or {@link HoundedRoundStopEvent} for one kind. The
 * result has already been announced, and the server returns to the lobby right after.
 */
public abstract sealed class HoundedRoundEndEvent extends Event permits HoundedRoundWinEvent, HoundedRoundStopEvent {
    // Declared here only, so a listener for this class also receives both subclasses.
    private static final HandlerList HANDLERS = new HandlerList();

    private final List<UUID> runners;
    private final List<UUID> hunters;
    private final Duration huntTime;

    HoundedRoundEndEvent(List<UUID> runners, List<UUID> hunters, Duration huntTime) {
        this.runners = List.copyOf(runners);
        this.hunters = List.copyOf(hunters);
        this.huntTime = Objects.requireNonNull(huntTime, "huntTime");
    }

    /** @return every runner in the round, including those who were eliminated */
    public List<UUID> runners() {
        return runners;
    }

    /** @return every hunter in the round */
    public List<UUID> hunters() {
        return hunters;
    }

    /** @return time from the hunters' release to the end; zero if it ended during the headstart */
    public Duration huntTime() {
        return huntTime;
    }

    /** @return the handlers for this event and its subclasses */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handlers for this event and its subclasses; required by Bukkit */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
