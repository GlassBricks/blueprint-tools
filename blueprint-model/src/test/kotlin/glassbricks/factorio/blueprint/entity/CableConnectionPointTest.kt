package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Position
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class CableConnectionPointTest {
    lateinit var point1: CableConnectionPoint
    lateinit var point2: CableConnectionPoint
    lateinit var point3: CableConnectionPoint

    @BeforeEach
    fun setUp() {
        point1 = UnknownEntity("entity", Position.ZERO)
        point2 = UnknownEntity("entity", Position.ZERO)
        point3 = UnknownEntity("entity", Position.ZERO)
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

    @Test
    fun `adding a point to itself has no effect`() {
        point1.cableConnections.add(point1)
        assertFalse(point1.cableConnections.contains(point1))
    }

    @Test
    fun `connecting a power switch to itself has no effect`() {
        val powerSwitch = loadEntity("power-switch") as PowerSwitch
        powerSwitch.left.cableConnections.add(powerSwitch.right)
        assertFalse(powerSwitch.left.cableConnections.contains(powerSwitch.right))
    }
}

class ElectricPoleTest {
    @Test
    fun `can create ElectricPole`() {
        testSaveLoad<ElectricPole>("small-electric-pole")
    }
}

class PowerSwitchTest {
    @Test
    fun `can create PowerSwitch`() {
        testSaveLoad<PowerSwitch>("power-switch") {
            switch_state  = true
        }
    }

    @Test
    fun `can connect to pole`() {
        val pole = loadEntity("small-electric-pole") as ElectricPole
        val powerSwitch = loadEntity("power-switch") as PowerSwitch

        pole.cableConnections.add(powerSwitch.left)
        assertTrue(powerSwitch.left in pole.cableConnections)
        assertTrue(pole in powerSwitch.left.cableConnections)
    }
}
