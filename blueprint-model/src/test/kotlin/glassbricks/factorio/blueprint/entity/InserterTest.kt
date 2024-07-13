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
        testSaveLoad(Inserter::class, "inserter")
        val defaultInserter = testSaveLoad(
            Inserter::class,
            "inserter",
            connectToNetwork = true,
            build = fun EntityJson.() {
                control_behavior = ControlBehaviorJson(
                    circuit_mode_of_operation = InserterModeOfOperation.SetFilters.asMode(),
                    circuit_set_stack_size = true
                )
            })
        assertEquals(
            defaultInserter.controlBehavior.defaultStackSizeSignal,
            defaultInserter.controlBehavior.setStackSizeSignal
        )
        testSaveLoad(Inserter::class, "inserter", connectToNetwork = true, build = fun EntityJson.() {
            control_behavior = ControlBehaviorJson(
                circuit_mode_of_operation = InserterModeOfOperation.None.asMode(),
            )
        })
        testSaveLoad(Inserter::class, "fast-inserter", null, false, fun EntityJson.() {
            override_stack_size = 2U
        })
        testSaveLoad(Inserter::class, "long-handed-inserter", connectToNetwork = true, build = fun EntityJson.() {
            override_stack_size = 3U
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(comparator = CompareOperation.Equal),
                circuit_mode_of_operation = InserterModeOfOperation.EnableDisable.asMode(),
                circuit_read_hand_contents = false,
            )
        })
        testSaveLoad(Inserter::class, "filter-inserter", null, false, fun EntityJson.() {
            filters = itemFilterList("copper-plate", "iron-plate", null, "steel-plate")
            drop_position = Position(1.0, 2.0)
            pickup_position = Position(3.0, 4.0)
        })
    }
}
