package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import kotlin.test.Test

class LampTest {
    @Test
    fun `can load lamp`() {
        testSaveLoad(Lamp::class, "small-lamp")
        testSaveLoad(Lamp::class, "small-lamp", connectToNetwork = true)
        testSaveLoad(Lamp::class, "small-lamp", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.GreaterOrEqual,
                    constant = 5,
                ),
                use_colors = true
            )
        }
    }
}
