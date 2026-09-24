package dev.marshall.hounded.config;

/** Keys in {@code messages.yml}. */
public enum MessageKey {
    PREFIX("prefix"),
    QUICK_START_GUIDE("quick-start-guide"),
    HELP("help"),
    RELOAD_SUCCESS("reload.success"),
    RELOAD_FAILED("reload.failed"),
    ROLE_NAME_RUNNER("role.name.runner"),
    ROLE_NAME_HUNTER("role.name.hunter"),
    ROLE_NAME_RUNNERS("role.name.runners"),
    ROLE_NAME_HUNTERS("role.name.hunters"),
    ROLE_ASSIGNED("role.assigned"),
    ROLE_REMOVED("role.removed"),
    ROLE_NOT_ASSIGNED("role.not-assigned"),
    ROLE_CLEARED("role.cleared"),
    ROLE_LIST("role.list"),
    ROLE_LIST_EMPTY("role.list-empty"),
    ROLE_LOCKED("role.locked"),
    START_HEADSTART("start.headstart"),
    START_RELEASED("start.released"),
    START_FROZEN("start.frozen"),
    START_NO_RUNNERS("start.no-runners"),
    START_NO_HUNTERS("start.no-hunters"),
    START_ALREADY_RUNNING("start.already-running"),
    START_NEGATIVE_HEADSTART("start.negative-headstart"),
    STOP_STOPPED("stop.stopped"),
    STOP_NOT_RUNNING("stop.not-running"),
    ROUND_RUNNER_ELIMINATED("round.runner-eliminated"),
    ROUND_RUNNER_LEFT("round.runner-left"),
    ROUND_RUNNER_RETURNED("round.runner-returned"),
    ROUND_RUNNER_TIMED_OUT("round.runner-timed-out"),
    DISPLAY_TITLE("display.title"),
    DISPLAY_SEPARATOR("display.separator"),
    DISPLAY_HEADSTART("display.headstart"),
    DISPLAY_TIMER("display.timer"),
    DISPLAY_DISTANCE("display.distance"),
    DISPLAY_DISTANCE_PORTAL("display.distance-portal"),
    DISPLAY_NO_TRAIL("display.no-trail"),
    DISPLAY_TRACKING_OFF("display.tracking-off"),
    STATE_LOBBY("state.lobby"),
    STATE_HEADSTART("state.headstart"),
    STATE_RUNNING("state.running"),
    STATE_ENDED("state.ended"),
    DIMENSION_OVERWORLD("dimension.overworld"),
    DIMENSION_NETHER("dimension.nether"),
    DIMENSION_END("dimension.end"),
    WIN_RUNNERS("win.runners"),
    WIN_HUNTERS("win.hunters"),
    COMPASS_NAME("compass.name"),
    COMPASS_HOW_TO_USE_AUTO("compass.how-to-use-auto"),
    COMPASS_HOW_TO_USE_MANUAL("compass.how-to-use-manual"),
    COMPASS_NOT_IN_ROUND("compass.not-in-round"),
    COMPASS_NOW_TRACKING("compass.now-tracking"),
    COMPASS_NO_DATA("compass.no-data"),
    COMPASS_DISABLED_IN_NETHER("compass.disabled-in-nether"),
    ERROR_INTERNAL("error.internal"),
    ERROR_CANCELLED_BY_PLUGIN("error.cancelled-by-plugin");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
