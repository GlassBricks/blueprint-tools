package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import kotlin.test.Test
import kotlin.test.assertEquals

class RailSignalTest {
    @Test
    fun `can create signals`() {
        testSaveLoad(RailSignal::class, "rail-signal")
        val defaultRailSignal =
            testSaveLoad(RailSignal::class, "rail-signal", connectToNetwork = true) {
                control_behavior = ControlBehaviorJson(
                    circuit_read_signal = true,
                    circuit_close_signal = false,
                )
            }
        assertEquals(defaultRailSignal.controlBehavior.defaultRedSignal, defaultRailSignal.controlBehavior.redSignal)
        assertEquals(
            defaultRailSignal.controlBehavior.defaultOrangeSignal,
            defaultRailSignal.controlBehavior.orangeSignal
        )
        assertEquals(
            defaultRailSignal.controlBehavior.defaultGreenSignal,
            defaultRailSignal.controlBehavior.greenSignal
        )

        testSaveLoad(RailSignal::class, "rail-signal", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_read_signal = true,
                red_output_signal = signalId("signal-A"),
                orange_output_signal = signalId("signal-B"),
                green_output_signal = signalId("signal-C"),
                circuit_close_signal = true,
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.Less,
                    constant = 5
                )
            )
        }

        testSaveLoad(RailChainSignal::class, "rail-chain-signal")
        val defaultRailChainSignal = testSaveLoad(RailChainSignal::class, "rail-chain-signal", connectToNetwork = true)
        assertEquals(
            defaultRailChainSignal.controlBehavior.defaultRedSignal,
            defaultRailChainSignal.controlBehavior.redSignal
        )
        assertEquals(
            defaultRailChainSignal.controlBehavior.defaultOrangeSignal,
            defaultRailChainSignal.controlBehavior.orangeSignal
        )
        assertEquals(
            defaultRailChainSignal.controlBehavior.defaultGreenSignal,
            defaultRailChainSignal.controlBehavior.greenSignal
        )
        assertEquals(
            defaultRailChainSignal.controlBehavior.defaultBlueSignal,
            defaultRailChainSignal.controlBehavior.blueSignal
        )

        testSaveLoad(RailChainSignal::class, "rail-chain-signal", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                red_output_signal = signalId("signal-A"),
                orange_output_signal = signalId("signal-B"),
                green_output_signal = signalId("signal-C"),
                blue_output_signal = signalId("signal-D"),
            )
        }

    }
}
