package glassbricks.factorio.blueprint.placement.grid

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
    fun testAxis() {
        assertEquals(CardinalDirection.North.axis, Axis.NorthSouth)
        assertEquals(CardinalDirection.South.axis, Axis.NorthSouth)
        assertEquals(CardinalDirection.East.axis, Axis.EastWest)
        assertEquals(CardinalDirection.West.axis, Axis.EastWest)
    }

    @Test
    fun testShifted() {
        assertEquals(tilePos(1, 0), tilePos(0, 0).shifted(CardinalDirection.East))
        assertEquals(tilePos(0, 1), tilePos(0, 0).shifted(CardinalDirection.South))
        assertEquals(tilePos(-1, 0), tilePos(0, 0).shifted(CardinalDirection.West))
        assertEquals(tilePos(0, -1), tilePos(0, 0).shifted(CardinalDirection.North))
    }

    @Test
    fun testMapPerDirection() {
        val map = mapPerDirection { it.toString() }
        assertEquals(
            mapOf(
                CardinalDirection.North to "North",
                CardinalDirection.East to "East",
                CardinalDirection.South to "South",
                CardinalDirection.West to "West"
            ), map
        )
    }
}
