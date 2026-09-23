package dev.marshall.hounded.tracking;

import dev.marshall.hounded.game.GameSession;
import java.util.Objects;

/**
 * Keeps every hunter's compass pointing at their chosen runner.
 *
 * <p>TODO(scaffold): not implemented yet. Planned behaviour:
 *
 * <ul>
 *   <li>{@link #start()} schedules a main-thread repeating task at the configured update interval
 *       (auto mode); manual mode updates only on right-click.
 *   <li>Each update builds a {@link TrackingSnapshot} from Bukkit state, asks {@link
 *       TargetResolver}, and hands the result to {@link CompassItem#pointAt}.
 *   <li>Tracks per-hunter selected runner (left-click cycles) and per-runner portal memory; both are
 *       cleared on player quit and on {@link #stop()}.
 * </ul>
 */
public final class TrackingService {
    private final GameSession session;
    private final TargetResolver resolver;
    private final CompassItem compassItem;

    public TrackingService(GameSession session, TargetResolver resolver, CompassItem compassItem) {
        this.session = Objects.requireNonNull(session, "session");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.compassItem = Objects.requireNonNull(compassItem, "compassItem");
    }

    /** TODO(scaffold): schedule the update task. */
    public void start() {}

    /** Cancels the update task and forgets per-player state. Safe to call when not started. */
    public void stop() {
        // TODO(scaffold): cancel the task and clear per-player state once they exist.
    }
}
