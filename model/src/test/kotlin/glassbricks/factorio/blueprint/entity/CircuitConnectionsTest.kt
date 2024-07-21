package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.CircuitID
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun testConnectable() = UnknownEntity("test", Position.ZERO)

class CircuitConnectionsTest {
    lateinit var point1: CircuitConnectionPoint
    lateinit var point2: CircuitConnectionPoint
    lateinit var point3: CircuitConnectionPoint

    @BeforeEach
    fun setUp() {
        val e1 = testConnectable()
        point1 = e1
        point2 = object : CircuitConnectionPoint {
            override val circuitConnections: CircuitConnections = CircuitConnections(this)
            override val circuitID: CircuitID get() = CircuitID.Second
            override val entity: BlueprintEntity = e1
        }
        point3 = testConnectable()
    }

    @Test
    fun `isEmpty returns if connections are empty`() {
        assertTrue(point1.circuitConnections.isEmpty())
        assertTrue(point2.circuitConnections.isEmpty())
        point1.circuitConnections.red.add(point2)
        assertFalse(point1.circuitConnections.isEmpty())
        assertFalse(point2.circuitConnections.isEmpty())
    }

    @Test
    fun `adding a connection also adds the reverse connection`() {
        point1.circuitConnections.red.add(point2)
        assertTrue(point2.circuitConnections.red.contains(point1))
        assertFalse(point2.circuitConnections.green.contains(point1))

        point2.circuitConnections.green.add(point1)
        assertTrue(point1.circuitConnections.green.contains(point2))
        assertTrue(point1.circuitConnections.red.contains(point2))
    }

    @Test
    fun `adding a point to itself has no effect`() {
        point1.circuitConnections.red.add(point1)
        assertFalse(point1.circuitConnections.red.contains(point1))

        point2.circuitConnections.red.add(point2)
        assertFalse(point2.circuitConnections.red.contains(point2))
    }

    @Test
    fun `can connect a point to other point on the same entity`() {
        point1.circuitConnections.red.add(point2)
        assertTrue(point1.circuitConnections.red.contains(point2))
        assertTrue(point2.circuitConnections.red.contains(point1))
    }

    @Test
    fun `removing a connection also removes the reverse connection`() {
        point1.circuitConnections.red.add(point2)
        point2.circuitConnections.red.remove(point1)

        assertFalse(point2.circuitConnections.green.contains(point1))
        assertFalse(point1.circuitConnections.red.contains(point2))
    }

    @Test
    fun `clear removes all connections`() {
        point1.circuitConnections.red.add(point2)
        point1.circuitConnections.green.add(point3)

        point1.circuitConnections.clear()
        assertFalse(point1.circuitConnections.red.contains(point2))
        assertFalse(point1.circuitConnections.green.contains(point3))
        assertFalse(point3.circuitConnections.green.contains(point1))
        assertFalse(point2.circuitConnections.red.contains(point1))
    }
}
