package dev.marshall.hounded.config;

/** Keys in {@code messages.yml}. */
public enum MessageKey {
    PREFIX("prefix"),
    QUICK_START_GUIDE("quick-start-guide"),
    RELOAD_SUCCESS("reload.success"),
    RELOAD_FAILED("reload.failed"),
    ROLE_ASSIGNED("role.assigned"),
    ROLE_REMOVED("role.removed"),
    ROLE_CLEARED("role.cleared"),
    ROLE_LIST("role.list"),
    ROLE_LIST_EMPTY("role.list-empty"),
    ROLE_LOCKED("role.locked"),
    START_HEADSTART("start.headstart"),
    START_RELEASED("start.released"),
    START_NO_RUNNERS("start.no-runners"),
    START_NO_HUNTERS("start.no-hunters"),
    START_ALREADY_RUNNING("start.already-running"),
    START_NEGATIVE_HEADSTART("start.negative-headstart"),
    STOP_STOPPED("stop.stopped"),
    STOP_NOT_RUNNING("stop.not-running"),
    WIN_RUNNERS("win.runners"),
    WIN_HUNTERS("win.hunters"),
    COMPASS_NAME("compass.name"),
    COMPASS_HOW_TO_USE("compass.how-to-use"),
    COMPASS_NO_DATA("compass.no-data"),
    COMPASS_DISABLED_IN_NETHER("compass.disabled-in-nether"),
    ERROR_NO_PERMISSION("error.no-permission"),
    ERROR_PLAYER_NOT_FOUND("error.player-not-found");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
