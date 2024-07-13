package glassbricks.factorio.blueprint

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundingBoxTest {
    @Test
    fun testBoundingBox() {
        val boundingBox = bbox(pos(0.0, 1.0), pos(10.0, 12.0))

        assertEquals(0.0, boundingBox.minX)
        assertEquals(1.0, boundingBox.minY)
        assertEquals(10.0, boundingBox.maxX)
        assertEquals(12.0, boundingBox.maxY)

        assertEquals(10.0, boundingBox.width)
        assertEquals(11.0, boundingBox.height)

        assertEquals("BoundingBox(pos(0.0, 1.0), pos(10.0, 12.0))", boundingBox.toString())
    }

    @Test
    fun alternateConstructor() {
        val boundingBox = BoundingBox(pos(0.0, 1.0), pos(10.0, 12.0))

        assertEquals(boundingBox, bbox(0.0, 1.0, 10.0, 12.0))
    }

    @Test
    fun contains() {
        val boundingBox = bbox(pos(0.0, 1.0), pos(10.0, 12.0))
        assertTrue(pos(5.0, 6.0) in boundingBox)
        assertFalse(pos(11.0, 12.0) in boundingBox)
    }

    @Test
    fun translate() {
        val boundingBox = bbox(pos(0.0, 1.0), pos(10.0, 12.0))
        val translated = boundingBox.translate(pos(5.0, 6.0))
        val expected = bbox(5.0, 7.0, 15.0, 18.0)
        assertEquals(expected, translated)
    }

    @Test
    fun rotateCardinal() {
        val boundingBox = bbox(-1.0, -2.0, 3.0, 4.0)
        assertEquals(boundingBox, boundingBox.rotateCardinal(Direction.North))
        assertEquals(boundingBox, boundingBox.rotateCardinal(Direction.Northeast))
        assertEquals(bbox(-4.0, -1.0, 2.0, 3.0), boundingBox.rotateCardinal(Direction.East))
        assertEquals(boundingBox.rotateCardinal(Direction.East), boundingBox.rotateCardinal(Direction.Southeast))
        assertEquals(bbox(-3.0, -4.0, 1.0, 2.0), boundingBox.rotateCardinal(Direction.South))
        assertEquals(boundingBox.rotateCardinal(Direction.South), boundingBox.rotateCardinal(Direction.Southwest))
        assertEquals(bbox(-2.0, -3.0, 4.0, 1.0), boundingBox.rotateCardinal(Direction.West))
        assertEquals(boundingBox.rotateCardinal(Direction.West), boundingBox.rotateCardinal(Direction.Northwest))
    }

    @Test
    fun around() {
        val around = BoundingBox.around(pos(5.0, 6.0), 5.0)
        val expected = bbox(0.0, 1.0, 10.0, 11.0)
        assertEquals(expected, around)
    }
}
