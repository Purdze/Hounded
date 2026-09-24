package dev.marshall.hounded.api;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Fired when an admin stops a round with {@code /hounded stop}. Nobody won. Cannot be cancelled. */
public final class HoundedRoundStopEvent extends HoundedRoundEndEvent {

    /**
     * Creates the event. Only Hounded fires it.
     *
     * @param runners every runner in the round
     * @param hunters every hunter in the round
     * @param huntTime time from the hunters' release to the stop
     */
    public HoundedRoundStopEvent(List<UUID> runners, List<UUID> hunters, Duration huntTime) {
        super(runners, hunters, huntTime);
    }
}
