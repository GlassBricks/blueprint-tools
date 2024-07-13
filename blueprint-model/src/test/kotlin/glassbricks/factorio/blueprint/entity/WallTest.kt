package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import kotlin.test.Test

class WallTest {
    @Test
    fun `can load wall`() {
        testSaveLoad(Wall::class, "stone-wall")
        testSaveLoad(Wall::class, "stone-wall", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.Less,
                    constant = 5
                ),
                circuit_open_gate = true,
                circuit_read_sensor = true
            )
        }
    }
}
