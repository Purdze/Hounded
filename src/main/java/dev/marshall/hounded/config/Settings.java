package dev.marshall.hounded.config;

import java.util.Objects;

/**
 * Immutable, validated view of {@code config.yml}, grouped like the file's sections. Replaced as a
 * whole on reload.
 */
public record Settings(
        Headstart headstart, Compass compass, Rules rules, Display display, boolean showQuickStartGuide) {

    /** Used when a value is missing or invalid; must match the bundled {@code config.yml}. */
    public static final Settings DEFAULTS = new Settings(
            new Headstart(30, true, true),
            new Compass(CompassUpdateMode.AUTO, 20, false),
            new Rules(false, true, false, true, 300),
            new Display(DisplayMode.BOSSBAR, true),
            true);

    public Settings {
        Objects.requireNonNull(headstart, "headstart");
        Objects.requireNonNull(compass, "compass");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(display, "display");
    }

    public record Headstart(int defaultSeconds, boolean freezeHunters, boolean blindHunters) {
        public Headstart {
            requireAtLeast(defaultSeconds, 0, "defaultSeconds");
        }
    }

    public record Compass(CompassUpdateMode updateMode, int updateIntervalTicks, boolean disableInNetherForHunters) {
        public Compass {
            Objects.requireNonNull(updateMode, "updateMode");
            requireAtLeast(updateIntervalTicks, 1, "updateIntervalTicks");
        }
    }

    public record Rules(
            boolean freezeWhenLookedAt,
            boolean runnerCanAttackHunters,
            boolean friendlyFire,
            boolean eliminatedRunnersSpectate,
            int runnerRejoinGraceSeconds) {
        public Rules {
            requireAtLeast(runnerRejoinGraceSeconds, 0, "runnerRejoinGraceSeconds");
        }
    }

    public record Display(DisplayMode mode, boolean showDistance) {
        public Display {
            Objects.requireNonNull(mode, "mode");
        }
    }

    private static void requireAtLeast(int value, int minimum, String name) {
        if (value < minimum) {
            throw new IllegalArgumentException(name + " must be >= " + minimum);
        }
    }
}
