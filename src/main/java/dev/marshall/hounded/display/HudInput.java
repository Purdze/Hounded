package dev.marshall.hounded.display;

import dev.marshall.hounded.game.GameState;
import dev.marshall.hounded.game.Role;
import dev.marshall.hounded.tracking.TrackingReading;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything {@link HudComposer} needs for one player.
 *
 * @param role empty if the player is not in the round
 * @param tracking what the player's compass shows; only hunters have one
 */
public record HudInput(
        GameState state,
        Duration headstartRemaining,
        Duration headstartLength,
        Duration elapsedHuntTime,
        Optional<Role> role,
        Optional<Tracking> tracking,
        boolean showDistance) {

    /** A compass reading together with the tracked runner's display name. */
    public record Tracking(TrackingReading reading, String runnerName) {
        public Tracking {
            Objects.requireNonNull(reading, "reading");
            Objects.requireNonNull(runnerName, "runnerName");
        }
    }

    public HudInput {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(headstartRemaining, "headstartRemaining");
        Objects.requireNonNull(headstartLength, "headstartLength");
        Objects.requireNonNull(elapsedHuntTime, "elapsedHuntTime");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(tracking, "tracking");
    }
}
