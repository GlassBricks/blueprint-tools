package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import kotlin.test.Test
import kotlin.test.assertTrue

class PowerSwitchTest {
    @Test
    fun `can create a power switch`() {
        testSaveLoad(PowerSwitch::class, "power-switch", null, false, fun EntityJson.() {
            switch_state = true
        })
        testSaveLoad(PowerSwitch::class, "power-switch", connectToNetwork = true, build = fun EntityJson.() {
            switch_state = false
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.Less,
                    constant = 5
                ),
            )
        })

    }

    @Test
    fun `can connect to pole`() {
        val pole = loadEntity<Any>("small-electric-pole") as ElectricPole
        val powerSwitch = loadEntity<Any>("power-switch") as PowerSwitch

        pole.cableConnections.add(powerSwitch.left)
        assertTrue(powerSwitch.left in pole.cableConnections)
        assertTrue(pole in powerSwitch.left.cableConnections)
    }
}
