package dev.marshall.hounded.game;

/** Why a requested change to the session was refused. Callers map these to message keys. */
public enum RejectionReason {
    NOT_IN_LOBBY,
    NOT_ACTIVE,
    NOT_ENDED,
    NO_RUNNERS,
    NO_HUNTERS,
    NEGATIVE_HEADSTART,
    HEADSTART_TOO_LONG,
    NOT_A_RUNNER,
    NOT_IN_ROLE,
    ALREADY_ELIMINATED,
    NOT_AWAITING_RETURN,
    ROLES_LOCKED,
    CANCELLED_BY_PLUGIN
}
