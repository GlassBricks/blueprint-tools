package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import kotlin.test.Test

class LampTest {
    @Test
    fun `can load lamp`() {
        testSaveLoad(Lamp::class, "small-lamp")
        testSaveLoad(Lamp::class, "small-lamp", connectToNetwork = true, build = fun EntityJson.() {

        })
        testSaveLoad(Lamp::class, "small-lamp", connectToNetwork = true, build = fun EntityJson.() {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.GreaterOrEqual,
                    constant = 5,
                ),
                use_colors = true
            )
        })
    }
}
