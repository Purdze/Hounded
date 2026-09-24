package dev.marshall.hounded.config;

import dev.marshall.hounded.game.Role;

/** The translatable names of each role, as message keys. */
public final class RoleMessages {

    private RoleMessages() {}

    public static MessageKey singular(Role role) {
        return switch (role) {
            case RUNNER -> MessageKey.ROLE_NAME_RUNNER;
            case HUNTER -> MessageKey.ROLE_NAME_HUNTER;
        };
    }

    public static MessageKey plural(Role role) {
        return switch (role) {
            case RUNNER -> MessageKey.ROLE_NAME_RUNNERS;
            case HUNTER -> MessageKey.ROLE_NAME_HUNTERS;
        };
    }
}
