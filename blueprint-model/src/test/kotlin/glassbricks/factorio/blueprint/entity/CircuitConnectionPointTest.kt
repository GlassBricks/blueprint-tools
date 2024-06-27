package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Position
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun testConnectable() = UnknownEntity("test", Position.ZERO)

class CircuitConnectionPointTest {
    lateinit var point1: CircuitConnectionPoint
    lateinit var point2: CircuitConnectionPoint
    lateinit var point3: CircuitConnectionPoint

    @BeforeEach
    fun setUp() {
        val e1 = testConnectable()
        point1 = e1.connectionPoint1
        point2 = e1.connectionPoint2
        point3 = testConnectable().connectionPoint2
    }

    @Test
    fun `isEmpty returns if connections are empty`() {
        assertTrue(point1.isEmpty())
        assertTrue(point2.isEmpty())
        point1.red.add(point2)
        assertFalse(point1.isEmpty())
        assertFalse(point2.isEmpty())
    }

    @Test
    fun `adding a connection also adds the reverse connection`() {
        point1.red.add(point2)
        assertTrue(point2.red.contains(point1))
        assertFalse(point2.green.contains(point1))

        point2.green.add(point1)
        assertTrue(point1.green.contains(point2))
        assertTrue(point1.red.contains(point2))
    }

    @Test
    fun `adding a point to itself has no effect`() {
        point1.red.add(point1)
        assertFalse(point1.red.contains(point1))
        
        point2.red.add(point2)
        assertFalse(point2.red.contains(point2))
    }
    
    @Test
    fun `can connect a point to other point on the same entity`() {
        point1.red.add(point2)
        assertTrue(point1.red.contains(point2))
        assertTrue(point2.red.contains(point1))
    }

    @Test
    fun `removing a connection also removes the reverse connection`() {
        point1.red.add(point2)
        point2.red.remove(point1)

        assertFalse(point2.green.contains(point1))
        assertFalse(point1.red.contains(point2))
    }

    @Test
    fun `clear removes all connections`() {
        point1.red.add(point2)
        point1.green.add(point3)

        point1.clear()
        assertFalse(point1.red.contains(point2))
        assertFalse(point1.green.contains(point3))
        assertFalse(point3.green.contains(point1))
        assertFalse(point2.red.contains(point1))
    }
}
