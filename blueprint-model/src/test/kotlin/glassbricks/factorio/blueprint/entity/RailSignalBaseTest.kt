package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.json.SignalType
import kotlin.test.Test

fun signalId(name: String) = SignalID(
    name = name, type = SignalType.Virtual
)

class RailSignalBaseTest {
    @Test
    fun `can create signals`() {
        testSaveLoad<RailSignal>("rail-signal")
        testSaveLoad<RailSignal>("rail-signal", connectToNetwork = true) {
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

        testSaveLoad<RailChainSignal>("rail-chain-signal")
        testSaveLoad<RailChainSignal>("rail-chain-signal", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                red_output_signal = signalId("signal-A"),
                orange_output_signal = signalId("signal-B"),
                green_output_signal = signalId("signal-C"),
                blue_output_signal = signalId("signal-D"),
            )
        }

    }
}
