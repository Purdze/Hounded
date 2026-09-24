package dev.marshall.hounded.command;

import dev.marshall.hounded.config.MessageKey;
import dev.marshall.hounded.game.RejectionReason;
import dev.marshall.hounded.game.TransitionResult;
import java.util.Optional;

/** Which message explains a rejection. Exhaustive, so a new reason must be given a message. */
final class RejectionMessages {

    private RejectionMessages() {}

    /** @return the message explaining why {@code result} was refused; empty if it wasn't */
    static Optional<MessageKey> keyFor(TransitionResult result) {
        return result instanceof TransitionResult.Rejected rejected
                ? Optional.of(keyFor(rejected.reason()))
                : Optional.empty();
    }

    private static MessageKey keyFor(RejectionReason reason) {
        return switch (reason) {
            case NOT_IN_LOBBY -> MessageKey.START_ALREADY_RUNNING;
            case NOT_ACTIVE -> MessageKey.STOP_NOT_RUNNING;
            case NO_RUNNERS -> MessageKey.START_NO_RUNNERS;
            case NO_HUNTERS -> MessageKey.START_NO_HUNTERS;
            case NEGATIVE_HEADSTART -> MessageKey.START_NEGATIVE_HEADSTART;
            case HEADSTART_TOO_LONG -> MessageKey.START_HEADSTART_TOO_LONG;
            case ROLES_LOCKED -> MessageKey.ROLE_LOCKED;
            case NOT_IN_ROLE -> MessageKey.ROLE_NOT_ASSIGNED;
            case CANCELLED_BY_PLUGIN -> MessageKey.ERROR_CANCELLED_BY_PLUGIN;
            // Only listeners trigger these; reaching one from a command is a bug.
            case NOT_ENDED, NOT_A_RUNNER, ALREADY_ELIMINATED, NOT_AWAITING_RETURN -> MessageKey.ERROR_INTERNAL;
        };
    }
}
