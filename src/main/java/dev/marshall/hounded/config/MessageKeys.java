package dev.marshall.hounded.config;

import java.util.List;

/** Keys in {@code messages.yml}. Every constant must also be listed in {@link #ALL}. */
public final class MessageKeys {
    public static final String PREFIX = "prefix";
    public static final String QUICK_START_GUIDE = "quick-start-guide";
    public static final String RELOAD_SUCCESS = "reload.success";
    public static final String RELOAD_FAILED = "reload.failed";
    public static final String ROLE_ASSIGNED = "role.assigned";
    public static final String ROLE_REMOVED = "role.removed";
    public static final String ROLE_CLEARED = "role.cleared";
    public static final String ROLE_LIST = "role.list";
    public static final String ROLE_LIST_EMPTY = "role.list-empty";
    public static final String ROLE_LOCKED = "role.locked";
    public static final String START_HEADSTART = "start.headstart";
    public static final String START_RELEASED = "start.released";
    public static final String START_NO_RUNNERS = "start.no-runners";
    public static final String START_NO_HUNTERS = "start.no-hunters";
    public static final String START_ALREADY_RUNNING = "start.already-running";
    public static final String START_NEGATIVE_HEADSTART = "start.negative-headstart";
    public static final String STOP_STOPPED = "stop.stopped";
    public static final String STOP_NOT_RUNNING = "stop.not-running";
    public static final String WIN_RUNNERS = "win.runners";
    public static final String WIN_HUNTERS = "win.hunters";
    public static final String COMPASS_NAME = "compass.name";
    public static final String COMPASS_HOW_TO_USE = "compass.how-to-use";
    public static final String COMPASS_NO_DATA = "compass.no-data";
    public static final String COMPASS_DISABLED_IN_NETHER = "compass.disabled-in-nether";
    public static final String ERROR_NO_PERMISSION = "error.no-permission";
    public static final String ERROR_PLAYER_NOT_FOUND = "error.player-not-found";

    public static final List<String> ALL = List.of(
            PREFIX,
            QUICK_START_GUIDE,
            RELOAD_SUCCESS,
            RELOAD_FAILED,
            ROLE_ASSIGNED,
            ROLE_REMOVED,
            ROLE_CLEARED,
            ROLE_LIST,
            ROLE_LIST_EMPTY,
            ROLE_LOCKED,
            START_HEADSTART,
            START_RELEASED,
            START_NO_RUNNERS,
            START_NO_HUNTERS,
            START_ALREADY_RUNNING,
            START_NEGATIVE_HEADSTART,
            STOP_STOPPED,
            STOP_NOT_RUNNING,
            WIN_RUNNERS,
            WIN_HUNTERS,
            COMPASS_NAME,
            COMPASS_HOW_TO_USE,
            COMPASS_NO_DATA,
            COMPASS_DISABLED_IN_NETHER,
            ERROR_NO_PERMISSION,
            ERROR_PLAYER_NOT_FOUND);

    private MessageKeys() {}
}
