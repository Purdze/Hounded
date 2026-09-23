package dev.marshall.hounded.config;

import java.util.Objects;

/** Immutable, validated view of {@code config.yml}. Replaced as a whole on reload. */
public record Settings(
        int defaultHeadstartSeconds,
        CompassUpdateMode compassUpdateMode,
        int compassUpdateIntervalTicks,
        boolean disableCompassInNetherForHunters,
        boolean freezeWhenLookedAt,
        boolean runnerCanAttackHunters,
        boolean friendlyFire,
        DisplayMode displayMode,
        boolean showDistance,
        boolean showQuickStartGuide) {

    /** Used when a value is missing or invalid; must match the bundled {@code config.yml}. */
    public static final Settings DEFAULTS =
            new Settings(30, CompassUpdateMode.AUTO, 20, false, false, true, false, DisplayMode.BOSSBAR, true, true);

    public Settings {
        Objects.requireNonNull(compassUpdateMode, "compassUpdateMode");
        Objects.requireNonNull(displayMode, "displayMode");
        if (defaultHeadstartSeconds < 0) {
            throw new IllegalArgumentException("defaultHeadstartSeconds must be >= 0");
        }
        if (compassUpdateIntervalTicks < 1) {
            throw new IllegalArgumentException("compassUpdateIntervalTicks must be >= 1");
        }
    }
}
