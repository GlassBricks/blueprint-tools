package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import kotlin.test.Test

private fun signalID(name: String) = SignalID(name=name, type=SignalType.Item)

class TrainStopTest {
    @Test
    fun `can load train stop`() {
        testSaveLoad<TrainStop>("train-stop")
        testSaveLoad<TrainStop>("train-stop", connectToNetwork = true) {
            control_behavior = ControlBehavior()
        }
        testSaveLoad<TrainStop>("train-stop") {
            station = "foo"
            color = Color(0.5, 0.5, 0.5)
            manual_trains_limit = 5
        }
        testSaveLoad<TrainStop>("train-stop", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                send_to_train = true,
                read_from_train = true,
                circuit_enable_disable = true,
                circuit_condition = CircuitCondition(
                    first_signal = signalID("signal-A"),
                    comparator = CompareOperation.GreaterOrEqual,
                    constant = 5,
                ),
                read_stopped_train = true,
                train_stopped_signal = signalID("signal-B"),
                set_trains_limit = true,
                trains_limit_signal = signalID("signal-C"),
                read_trains_count = true,
                trains_count_signal = signalID("signal-D")
            )
        }
    }
}
