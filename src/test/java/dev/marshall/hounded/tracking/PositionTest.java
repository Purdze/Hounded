package dev.marshall.hounded.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void horizontalDistanceIgnoresHeight() {
        assertEquals(5.0, new Position(0, 10, 0).horizontalDistanceTo(new Position(3, 200, 4)));
    }

    @Test
    void distanceToItselfIsZero() {
        Position here = new Position(12.5, 64, -7);
        assertEquals(0.0, here.horizontalDistanceTo(here));
    }
}
