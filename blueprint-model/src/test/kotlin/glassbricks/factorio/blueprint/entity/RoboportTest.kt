package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.json.SignalType
import kotlin.test.Test

private fun signalID(name: String) = SignalID(name = name, type = SignalType.Item)

class RoboportTest {
    @Test
    fun `can load roboport`() {
        testSaveLoad<Roboport>("roboport")
        testSaveLoad<Roboport>("roboport", connectToNetwork = true)
        testSaveLoad<Roboport>("roboport", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                read_logistics = true,
                read_robot_stats = true,
                available_logistic_output_signal = signalID("signal-A"),
                total_logistic_output_signal = signalID("signal-B"),
                available_construction_output_signal = signalID("signal-C"),
                total_construction_output_signal = signalID("signal-D")
            )
        }
    }
}
