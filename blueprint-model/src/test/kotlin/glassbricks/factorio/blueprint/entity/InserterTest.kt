package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.InserterModeOfOperation
import glassbricks.factorio.blueprint.json.asMode
import kotlin.test.Test
import kotlin.test.assertEquals


class InserterTest {
    @Test
    fun `can load inserter`() {
        testSaveLoad<Inserter>("inserter")
        val defaultInserter = testSaveLoad<Inserter>("inserter", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_mode_of_operation = InserterModeOfOperation.SetFilters.asMode(),
                circuit_set_stack_size = true
            )
        }
        assertEquals(
            defaultInserter.controlBehavior.defaultStackSizeSignal,
            defaultInserter.controlBehavior.setStackSizeSignal
        )
        testSaveLoad<Inserter>("inserter", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                circuit_mode_of_operation = InserterModeOfOperation.None.asMode(),
            )
        }
        testSaveLoad<Inserter>("fast-inserter") {
            override_stack_size = 2U
        }
        testSaveLoad<Inserter>("long-handed-inserter", connectToNetwork = true) {
            override_stack_size = 3U
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(comparator = CompareOperation.Equal),
                circuit_mode_of_operation = InserterModeOfOperation.EnableDisable.asMode(),
                circuit_read_hand_contents = false,
            )
        }
        testSaveLoad<Inserter>("filter-inserter") {
            filters = itemFilterList("copper-plate", "iron-plate", null, "steel-plate")
            drop_position = Position(1.0, 2.0)
            pickup_position = Position(3.0, 4.0)
        }
    }
}
