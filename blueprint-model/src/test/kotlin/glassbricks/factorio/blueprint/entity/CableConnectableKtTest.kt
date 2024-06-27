package glassbricks.factorio.blueprint.entity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class CableConnectionSetTest {
    lateinit var point1: CableConnectionPoint
    lateinit var point2: CableConnectionPoint
    lateinit var point3: CableConnectionPoint

    private class C : CableConnectionPoint {
        override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    }

    @BeforeEach
    fun setUp() {
        point1 = C()
        point2 = C()
        point3 = C()
    }

    @Test
    fun `adding a connection also adds the reverse connection`() {
        point1.cableConnections.add(point2)
        assertTrue(point1.cableConnections.contains(point2))
        assertTrue(point2.cableConnections.contains(point1))
    }

    @Test
    fun `removing a connection also removes the reverse connection`() {
        point1.cableConnections.add(point2)
        point2.cableConnections.remove(point1)
        assertFalse(point1.cableConnections.contains(point2))
        assertFalse(point2.cableConnections.contains(point1))
    }
}

class ElectricPoleTest {
    @Test
    fun `can create ElectricPole`() {
        val pole = loadEntity("small-electric-pole")
        assert(pole is ElectricPole)
    }
}
