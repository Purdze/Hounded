package dev.marshall.hounded.game;

/** Lifecycle of a round: {@code LOBBY -> HEADSTART -> RUNNING -> ENDED -> LOBBY}. */
public enum GameState {
    LOBBY,
    HEADSTART,
    RUNNING,
    ENDED;

    /** Whether a round is in progress, i.e. roles are locked and win conditions apply. */
    public boolean isActive() {
        return this == HEADSTART || this == RUNNING;
    }
}
