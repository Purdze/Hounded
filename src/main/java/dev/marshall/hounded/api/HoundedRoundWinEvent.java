package dev.marshall.hounded.api;

import dev.marshall.hounded.game.Role;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired when a side wins: {@link Role#RUNNER} when the ender dragon dies, {@link Role#HUNTER} when
 * every runner is out. Cannot be cancelled.
 */
public final class HoundedRoundWinEvent extends HoundedRoundEndEvent {
    private final Role winner;

    /**
     * Creates the event. Only Hounded fires it.
     *
     * @param winner the side that won
     * @param runners every runner in the round
     * @param hunters every hunter in the round
     * @param huntTime time from the hunters' release to the win
     */
    public HoundedRoundWinEvent(Role winner, List<UUID> runners, List<UUID> hunters, Duration huntTime) {
        super(runners, hunters, huntTime);
        this.winner = Objects.requireNonNull(winner, "winner");
    }

    /** @return the side that won */
    public Role winner() {
        return winner;
    }
}
