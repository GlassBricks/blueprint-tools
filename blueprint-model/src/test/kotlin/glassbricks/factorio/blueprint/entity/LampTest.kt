package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import kotlin.test.Test

class LampTest {
    @Test
    fun `can load lamp`() {
        testSaveLoad<Lamp>("small-lamp")
        testSaveLoad<Lamp>("small-lamp", connectToNetwork = true) {}
        testSaveLoad<Lamp>("small-lamp", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = SignalID(name = "signal-A", type = SignalType.Item),
                    comparator = ComparatorString.GreaterOrEqual,
                    constant = 5,
                ),
                use_colors = true
            )
        }
    }
}
