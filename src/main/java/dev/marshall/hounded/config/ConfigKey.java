package dev.marshall.hounded.config;

/** Paths in {@code config.yml}. */
public enum ConfigKey {
    HEADSTART_DEFAULT_SECONDS("headstart.default-seconds"),
    COMPASS_UPDATE_MODE("compass.update-mode"),
    COMPASS_UPDATE_INTERVAL_TICKS("compass.update-interval-ticks"),
    COMPASS_DISABLE_IN_NETHER_FOR_HUNTERS("compass.disable-in-nether-for-hunters"),
    RULES_FREEZE_WHEN_LOOKED_AT("rules.freeze-when-looked-at"),
    RULES_RUNNER_CAN_ATTACK_HUNTERS("rules.runner-can-attack-hunters"),
    RULES_FRIENDLY_FIRE("rules.friendly-fire"),
    RULES_ELIMINATED_RUNNERS_SPECTATE("rules.eliminated-runners-spectate"),
    RULES_RUNNER_REJOIN_GRACE_SECONDS("rules.runner-rejoin-grace-seconds"),
    DISPLAY_MODE("display.mode"),
    DISPLAY_SHOW_DISTANCE("display.show-distance"),
    QUICK_START_GUIDE("quick-start-guide");

    private final String path;

    ConfigKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
