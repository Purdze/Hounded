package dev.marshall.hounded.tracking;

/**
 * The three vanilla dimensions. Pure Java on purpose, so {@link TargetResolver} stays free of
 * Bukkit types; the Bukkit side maps {@code World.Environment} onto this.
 */
public enum Dimension {
    OVERWORLD,
    NETHER,
    END
}
