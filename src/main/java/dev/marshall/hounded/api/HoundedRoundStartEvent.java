package dev.marshall.hounded.api;

import java.util.List;
import java.util.UUID;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread just before a round starts, after Hounded has checked that it can
 * start. Cancelling it keeps the server in the lobby, and the admin who started the round is told
 * another plugin cancelled it.
 */
public final class HoundedRoundStartEvent extends HoundedCancellableEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final List<UUID> runners;
    private final List<UUID> hunters;
    private final int headstartSeconds;

    /**
     * Creates the event. Only Hounded fires it.
     *
     * @param runners the runners, in the order they were added
     * @param hunters the hunters, in the order they were added
     * @param headstartSeconds the headstart the runners get; 0 for none
     */
    public HoundedRoundStartEvent(List<UUID> runners, List<UUID> hunters, int headstartSeconds) {
        this.runners = List.copyOf(runners);
        this.hunters = List.copyOf(hunters);
        this.headstartSeconds = headstartSeconds;
    }

    /** @return the runners' ids, in the order they were added; never empty */
    public List<UUID> runners() {
        return runners;
    }

    /** @return the hunters' ids, in the order they were added; never empty */
    public List<UUID> hunters() {
        return hunters;
    }

    /** @return seconds before the hunters are released; 0 means they are released at once */
    public int headstartSeconds() {
        return headstartSeconds;
    }

    /** @return the handlers for this event */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /** @return the handlers for this event; required by Bukkit */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
