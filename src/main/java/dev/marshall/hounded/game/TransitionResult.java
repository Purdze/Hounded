package dev.marshall.hounded.game;

import java.util.Objects;

/** Result of asking {@link GameSession} to change something. Returned instead of throwing. */
public sealed interface TransitionResult {

    record Changed(GameState from, GameState to) implements TransitionResult {
        public Changed {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }

    /** The request was valid but the state stayed the same (e.g. one of several runners died). */
    record Unchanged(GameState state) implements TransitionResult {
        public Unchanged {
            Objects.requireNonNull(state, "state");
        }
    }

    record Rejected(RejectionReason reason) implements TransitionResult {
        public Rejected {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
