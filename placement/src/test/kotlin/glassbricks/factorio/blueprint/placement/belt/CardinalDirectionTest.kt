package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.placement.Axis
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.shifted
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
        assertEquals(CardinalDirection.North.axis, Axis.Companion.NorthSouth)
        assertEquals(CardinalDirection.South.axis, Axis.Companion.NorthSouth)
        assertEquals(CardinalDirection.East.axis, Axis.Companion.EastWest)
        assertEquals(CardinalDirection.West.axis, Axis.Companion.EastWest)
    }

    @Test
    fun testShifted() {
        assertEquals(tilePos(1, 0), tilePos(0, 0).shifted(CardinalDirection.East))
        assertEquals(tilePos(0, 1), tilePos(0, 0).shifted(CardinalDirection.South))
        assertEquals(tilePos(-1, 0), tilePos(0, 0).shifted(CardinalDirection.West))
        assertEquals(tilePos(0, -1), tilePos(0, 0).shifted(CardinalDirection.North))
    }

}
