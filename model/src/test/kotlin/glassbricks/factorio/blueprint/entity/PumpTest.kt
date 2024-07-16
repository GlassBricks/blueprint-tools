package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import org.junit.jupiter.api.Test

class PumpTest {
    @Test
    fun `can load pump`() {
        testSaveLoad(Pump::class, "pump")
        testSaveLoad(Pump::class, "pump", connectToNetwork = true)
        testSaveLoad(Pump::class, "pump", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.GreaterOrEqual,
                    constant = 5,
                ),
            )
        }
    }
}
