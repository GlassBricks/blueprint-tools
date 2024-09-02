package glassbricks.factorio.blueprint

import kotlinx.serialization.json.Json
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PositionTest {
    @Test
    fun `creating a pos`() {
        val pos = pos(1.0, 2.0)
        assertEquals(1.0, pos.x)
        assertEquals(2.0, pos.y)
        assertEquals("pos(1.0, 2.0)", pos.toString())

        val pos2 = pos(1.25, 2 + 1 / 256.0)
        assertEquals(1.25, pos2.x)
        assertEquals(2 + 1 / 256.0, pos2.y)
    }

    @Test
    fun add() {
        val pos = pos(1.0, 2.0)
        val pos2 = pos(3.0, 4.0)
        val result = pos + pos2
        assertEquals(4.0, result.x)
        assertEquals(6.0, result.y)
    }

    @Test
    fun minus() {
        val pos = pos(1.0, 2.0)
        val pos2 = pos(3.0, 5.0)
        val result = pos - pos2
        assertEquals(-2.0, result.x)
        assertEquals(-3.0, result.y)
    }

    @Test
    fun timesDouble() {
        val pos = pos(1.0, 2.0)
        val result = pos * 1.5
        assertEquals(1.5, result.x)
        assertEquals(3.0, result.y)
    }

    @Test
    fun timesInt() {
        val result = pos(1.0, 2.0) * 3
        assertEquals(3.0, result.x)
        assertEquals(6.0, result.y)
    }

    @Test
    fun divDouble() {
        val pos = pos(3.0, 6.0)
        val result = pos / 1.5
        assertEquals(2.0, result.x)
        assertEquals(4.0, result.y)
    }

    @Test
    fun divInt() {
        val result = pos(3.0, 6.0) / 3
        assertEquals(1.0, result.x)
        assertEquals(2.0, result.y)
    }

    @Test
    fun unaryMinus() {
        val pos = pos(1.0, 2.0)
        val result = -pos
        assertEquals(-1.0, result.x)
        assertEquals(-2.0, result.y)
    }

    @Test
    fun length() {
        val pos = pos(3.0, 4.0)
        assertEquals(25.0, pos.squaredLength())
        assertEquals(5.0, pos.length())

        val bigPosition = pos(
            (1 shl 20) * 3.0,
            (1 shl 20) * 4.0
        )
        assertEquals((1L shl 40) * 25.0, bigPosition.squaredLength())
    }

    @Test
    fun distance() {
        val pos = pos(1.0, 2.0)
        val pos2 = pos(4.0, 6.0)
        assertEquals(25.0, pos.squaredDistanceTo(pos2))
        assertEquals(5.0, pos.distanceTo(pos2))
    }

    @Test
    fun serialize() {
        val pos = pos(1.0, 2.0)
        val json = Json.encodeToString(Position.serializer(), pos)
        assertEquals("""[1,2]""", json)
        val deserialized = Json.decodeFromString(Position.serializer(), json)
        assertEquals(pos, deserialized)
    }

    @Test
    fun occupiedTile() {
        assertEquals(TilePosition(1, 2), pos(1.0, 2.0).occupiedTile())
        assertEquals(TilePosition(2, 3), pos(2.9, 3.9).occupiedTile())
        assertEquals(TilePosition(-2, -3), pos(-1.9, -2.9).occupiedTile())
    }

    @Test
    fun rotateCardinal() {
        val pos = pos(1.0, 2.0)
        assertEquals(pos, pos.rotateCardinal(Direction.North))
        assertEquals(pos, pos.rotateCardinal(Direction.Northeast))
        assertEquals(pos(-2.0, 1.0), pos.rotateCardinal(Direction.East))
        assertEquals(pos(-2.0, 1.0), pos.rotateCardinal(Direction.Southeast))
        assertEquals(pos(-1.0, -2.0), pos.rotateCardinal(Direction.South))
        assertEquals(pos(-1.0, -2.0), pos.rotateCardinal(Direction.Southwest))
        assertEquals(pos(2.0, -1.0), pos.rotateCardinal(Direction.West))
        assertEquals(pos(2.0, -1.0), pos.rotateCardinal(Direction.Northwest))

        val unitX = pos(1.0, 0.0)
        assertEquals(unitX, unitX.rotateCardinal(Direction.North))
        assertEquals(pos(0.0, 1.0), unitX.rotateCardinal(Direction.East))
        assertEquals(pos(-1.0, 0.0), unitX.rotateCardinal(Direction.South))
        assertEquals(pos(0.0, -1.0), unitX.rotateCardinal(Direction.West))
        val unitY = pos(0.0, 1.0)
        assertEquals(unitY, unitY.rotateCardinal(Direction.North))
        assertEquals(pos(-1.0, 0.0), unitY.rotateCardinal(Direction.East))
        assertEquals(pos(0.0, -1.0), unitY.rotateCardinal(Direction.South))
        assertEquals(pos(1.0, 0.0), unitY.rotateCardinal(Direction.West))
    }
}

class VectorTest {
    @Test
    fun rotate() {
        val unitX = Vector(1.0, 0.0)
        assertEquals(unitX, unitX.rotate(Direction.North))
        assertEquals(Vector(-0.0, 1.0), unitX.rotate(Direction.East)) // -0.0 is needed for... reason
        assertEquals(Vector(-1.0, -0.0), unitX.rotate(Direction.South))
        assertEquals(Vector(0.0, -1.0), unitX.rotate(Direction.West))
        val sqrt2 = sin(Math.PI / 4)
        assertTrue(Vector(sqrt2, sqrt2).closeTo(unitX.rotate(Direction.Northeast)))
        assertTrue(Vector(-sqrt2, sqrt2).closeTo(unitX.rotate(Direction.Southeast)))
        assertTrue(Vector(-sqrt2, -sqrt2).closeTo(unitX.rotate(Direction.Southwest)))
        assertTrue(Vector(sqrt2, -sqrt2).closeTo(unitX.rotate(Direction.Northwest)))
    }
}

class TilePositionTest {
    @Test
    fun `creating a tile pos`() {
        val pos = TilePosition(1, 2)
        assertEquals(1, pos.x)
        assertEquals(2, pos.y)
    }

    @Test
    fun add() {
        val pos = TilePosition(1, 2)
        val pos2 = TilePosition(3, 4)
        val result = pos + pos2
        assertEquals(4, result.x)
        assertEquals(6, result.y)
    }

    @Test
    fun minus() {
        val pos = TilePosition(1, 2)
        val pos2 = TilePosition(3, 5)
        val result = pos - pos2
        assertEquals(-2, result.x)
        assertEquals(-3, result.y)
    }

    @Test
    fun timesInt() {
        val pos = TilePosition(1, 2)
        val result = pos * 3
        assertEquals(3, result.x)
        assertEquals(6, result.y)
    }

    @Test
    fun unaryMinus() {
        val pos = TilePosition(1, 2)
        val result = -pos
        assertEquals(-1, result.x)
        assertEquals(-2, result.y)
    }

    @Test
    fun length() {
        val pos = TilePosition(3, 4)
        assertEquals(25, pos.squaredLength())
        assertEquals(5.0, pos.length())
    }

    @Test
    fun center() {
        val pos = TilePosition(1, 2)
        assertEquals(pos(1.5, 2.5), pos.center())
    }

    @Test
    fun topLeftCorner() {
        val pos = TilePosition(1, 2)
        assertEquals(pos(1.0, 2.0), pos.topLeftCorner())
    }

    @Test
    fun isZero() {
        val pos = TilePosition(0, 0)
        assertEquals(true, pos.isZero())
        val pos2 = TilePosition(1, 0)
        assertEquals(false, pos2.isZero())
    }

    @Test
    fun tileBoundingBox() {
        val pos = TilePosition(1, 2)
        assertEquals(bbox(1.0, 2.0, 2.0, 3.0), pos.tileBoundingBox())
    }
}
