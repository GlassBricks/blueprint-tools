package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.ControlBehavior
import kotlin.test.Test

class TrainStopTest {
    @Test
    fun `can load train stop`() {
        testSaveLoad(TrainStop::class, "train-stop")
        testSaveLoad(TrainStop::class, "train-stop", connectToNetwork = true) {
            control_behavior = ControlBehavior()
        }
        testSaveLoad(TrainStop::class, "train-stop", null, false) {
            station = "foo"
            color = Color(0.5, 0.5, 0.5)
            manual_trains_limit = 5
        }
        testSaveLoad(TrainStop::class, "train-stop", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                send_to_train = true,
                read_from_train = true,
                circuit_enable_disable = true,
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.GreaterOrEqual,
                    constant = 5,
                ),
                read_stopped_train = true,
                train_stopped_signal = signalId("signal-B"),
                set_trains_limit = true,
                trains_limit_signal = signalId("signal-C"),
                read_trains_count = true,
                trains_count_signal = signalId("signal-D")
            )
        }
    }
}
