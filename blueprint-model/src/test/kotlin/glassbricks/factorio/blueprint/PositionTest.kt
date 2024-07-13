package glassbricks.factorio.blueprint

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PositionTest {
    @Test
    fun `creating a position`() {
        val position = Position(1.0, 2.0)
        assertEquals(1.0, position.x)
        assertEquals(2.0, position.y)
        assertEquals("Position(1.0, 2.0)", position.toString())

        val position2 = Position(1.25, 2 + 1 / 256.0)
        assertEquals(1.25, position2.x)
        assertEquals(2 + 1 / 256.0, position2.y)
    }

    @Test
    fun add() {
        val position = Position(1.0, 2.0)
        val position2 = Position(3.0, 4.0)
        val result = position + position2
        assertEquals(4.0, result.x)
        assertEquals(6.0, result.y)
    }

    @Test
    fun minus() {
        val position = Position(1.0, 2.0)
        val position2 = Position(3.0, 5.0)
        val result = position - position2
        assertEquals(-2.0, result.x)
        assertEquals(-3.0, result.y)
    }

    @Test
    fun timesDouble() {
        val position = Position(1.0, 2.0)
        val result = position * 1.5
        assertEquals(1.5, result.x)
        assertEquals(3.0, result.y)
    }

    @Test
    fun timesInt() {
        val position = Position(1.0, 2.0)
        val result = position * 3
        assertEquals(3.0, result.x)
        assertEquals(6.0, result.y)
    }

    @Test
    fun unaryMinus() {
        val position = Position(1.0, 2.0)
        val result = -position
        assertEquals(-1.0, result.x)
        assertEquals(-2.0, result.y)
    }

    @Test
    fun length() {
        val position = Position(3.0, 4.0)
        assertEquals(25.0, position.squaredLength())
        assertEquals(5.0, position.length())

        val bigPosition = Position(
            (1 shl 20) * 3.0,
            (1 shl 20) * 4.0
        )
        assertEquals((1L shl 40) * 25.0, bigPosition.squaredLength())
    }

    @Test
    fun distance() {
        val position = Position(1.0, 2.0)
        val position2 = Position(4.0, 6.0)
        assertEquals(25.0, position.squaredDistanceTo(position2))
        assertEquals(5.0, position.distanceTo(position2))
    }

    @Test
    fun serialize() {
        val position = Position(1.0, 2.0)
        val json = Json.encodeToString(Position.serializer(), position)
        assertEquals("""{"x":1,"y":2}""", json)
        val deserialized = Json.decodeFromString(Position.serializer(), json)
        assertEquals(position, deserialized)
    }
}
