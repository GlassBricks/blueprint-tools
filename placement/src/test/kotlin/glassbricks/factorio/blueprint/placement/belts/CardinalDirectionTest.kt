package glassbricks.factorio.blueprint.placement.belts

import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class CardinalDirectionTest {
    @Test
    fun testOppositeDir() {
        assertEquals(CardinalDirection.South, CardinalDirection.North.oppositeDir())
        assertEquals(CardinalDirection.West, CardinalDirection.East.oppositeDir())
        assertEquals(CardinalDirection.North, CardinalDirection.South.oppositeDir())
        assertEquals(CardinalDirection.East, CardinalDirection.West.oppositeDir())
    }

    @Test
    fun testShifted() {
        assertEquals(tilePos(1, 0), tilePos(0, 0).shifted(CardinalDirection.East))
        assertEquals(tilePos(0, 1), tilePos(0, 0).shifted(CardinalDirection.South))
        assertEquals(tilePos(-1, 0), tilePos(0, 0).shifted(CardinalDirection.West))
        assertEquals(tilePos(0, -1), tilePos(0, 0).shifted(CardinalDirection.North))
    }
}
