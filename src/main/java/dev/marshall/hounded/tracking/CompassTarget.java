package dev.marshall.hounded.tracking;

import java.util.Objects;

/** Where a hunter's compass should point, and why. */
public sealed interface CompassTarget {

    /** The runner is in the hunter's dimension: point straight at them. */
    record Runner(Position position) implements CompassTarget {
        public Runner {
            Objects.requireNonNull(position, "position");
        }
    }

    /** The runner is elsewhere: point at the portal they last left the hunter's dimension by. */
    record LastPortal(Position position) implements CompassTarget {
        public LastPortal {
            Objects.requireNonNull(position, "position");
        }
    }

    /**
     * Nothing useful is known yet. The caller falls back to the hunter's dimension spawn and tells
     * the hunter why, instead of leaving the compass spinning without explanation.
     */
    record NoData(Dimension hunterDimension) implements CompassTarget {
        public NoData {
            Objects.requireNonNull(hunterDimension, "hunterDimension");
        }
    }
}
