package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import kotlin.test.Test
import kotlin.test.assertEquals

class RoboportTest {
    @Test
    fun `can load roboport`() {
        testSaveLoad(Roboport::class, "roboport")
        testSaveLoad(Roboport::class, "roboport", connectToNetwork = true)
        val defaultRead = testSaveLoad(Roboport::class, "roboport", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                read_robot_stats = true
            )
        }
        assertEquals(
            defaultRead.controlBehavior.defaultAvailableLogisticSignal,
            defaultRead.controlBehavior.availableLogisticOutputSignal
        )
        assertEquals(
            defaultRead.controlBehavior.defaultTotalLogisticSignal,
            defaultRead.controlBehavior.totalLogisticOutputSignal
        )
        assertEquals(
            defaultRead.controlBehavior.defaultAvailableConstructionSignal,
            defaultRead.controlBehavior.availableConstructionOutputSignal
        )
        assertEquals(
            defaultRead.controlBehavior.defaultTotalConstructionSignal,
            defaultRead.controlBehavior.totalConstructionOutputSignal
        )

        testSaveLoad(Roboport::class, "roboport", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                read_logistics = true,
                read_robot_stats = true,
                available_logistic_output_signal = signalId("signal-A"),
                total_logistic_output_signal = signalId("signal-B"),
                available_construction_output_signal = signalId("signal-C"),
                total_construction_output_signal = signalId("signal-D")
            )
        }
        testSaveLoad(Roboport::class, "roboport", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                read_logistics = true,
                read_robot_stats = true,
                available_logistic_output_signal = nullSignalId,
                total_logistic_output_signal = nullSignalId,
                available_construction_output_signal = null,
                total_construction_output_signal = null
            )
        }
    }
}
