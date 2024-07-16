package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.*
import kotlin.test.Test


class InserterTest {
    @Test
    fun `can load inserter`() {
        testSaveLoad(Inserter::class, "inserter")
        testSaveLoad(Inserter::class, "inserter", connectToNetwork = true)
        testSaveLoad(Inserter::class, "inserter", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.GreaterOrEqual,
                    constant = 5,
                ),
            )
        }

        testSaveLoad(Inserter::class, "inserter", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_mode_of_operation = InserterModeOfOperation.None.asMode(),
            )
        }
        testSaveLoad(Inserter::class, "fast-inserter", null, false) {
            override_stack_size = 2U
        }
        testSaveLoad(Inserter::class, "long-handed-inserter", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(comparator = CompareOperation.Equal),
                circuit_read_hand_contents = true,
            )
        }
        testSaveLoad(Inserter::class, "long-handed-inserter", connectToNetwork = true) {
            override_stack_size = 3U
            control_behavior = ControlBehaviorJson(
                circuit_read_hand_contents = true,
                circuit_hand_read_mode = InserterHandReadMode.Hold,
            )
        }
        testSaveLoad(Inserter::class, "filter-inserter", null, false) {
            filters = itemFilterList("copper-plate", "iron-plate", null, "steel-plate")
            drop_position = Position(1.0, 2.0)
            pickup_position = Position(3.0, 4.0)
        }
    }
}
