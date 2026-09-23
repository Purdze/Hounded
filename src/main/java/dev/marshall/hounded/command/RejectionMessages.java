package dev.marshall.hounded.command;

import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.RejectionReason;

/** Which message explains a rejection. Exhaustive, so a new reason must be given a message. */
final class RejectionMessages {

    private RejectionMessages() {}

    static MessageKey keyFor(RejectionReason reason) {
        return switch (reason) {
            case NOT_IN_LOBBY -> MessageKey.START_ALREADY_RUNNING;
            case NOT_ACTIVE -> MessageKey.STOP_NOT_RUNNING;
            case NO_RUNNERS -> MessageKey.START_NO_RUNNERS;
            case NO_HUNTERS -> MessageKey.START_NO_HUNTERS;
            case NEGATIVE_HEADSTART -> MessageKey.START_NEGATIVE_HEADSTART;
            case ROLES_LOCKED -> MessageKey.ROLE_LOCKED;
            case NOT_IN_ROLE -> MessageKey.ROLE_NOT_ASSIGNED;
            // Only listeners trigger these; reaching one from a command is a bug.
            case NOT_ENDED, NOT_A_RUNNER, ALREADY_ELIMINATED -> MessageKey.ERROR_INTERNAL;
        };
    }
}
