package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.SignalType
import kotlin.test.Test

class RoboportTest {
    @Test
    fun `can load roboport`() {
        testSaveLoad<Roboport>("roboport")
        testSaveLoad<Roboport>("roboport", connectToNetwork = true)
        testSaveLoad<Roboport>("roboport", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                read_logistics = true,
                read_robot_stats = true,
                available_logistic_output_signal = signalId("signal-A"),
                total_logistic_output_signal = signalId("signal-B"),
                available_construction_output_signal = signalId("signal-C"),
                total_construction_output_signal = signalId("signal-D")
            )
        }
    }
}
