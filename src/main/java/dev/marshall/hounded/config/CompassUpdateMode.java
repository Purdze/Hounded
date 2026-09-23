package dev.marshall.hounded.config;

/** How hunters' compasses are refreshed. */
public enum CompassUpdateMode {
    /** Refreshed on a timer; see {@link Settings#compassUpdateIntervalTicks()}. */
    AUTO,
    /** Refreshed only when the hunter right-clicks. */
    MANUAL
}
