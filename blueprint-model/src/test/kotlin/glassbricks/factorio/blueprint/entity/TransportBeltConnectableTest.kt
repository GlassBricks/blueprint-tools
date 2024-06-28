package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.json.ControlBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class TransportBeltConnectableTest {
    @Test
    fun `can load transport belt`() {
        testSaveLoad<TransportBelt>("transport-belt")
        testSaveLoad<TransportBelt>("transport-belt", connectToNetwork = true) {
            control_behavior = ControlBehavior(
                circuit_enable_disable = false
            )
        }
        val belt = testSaveLoad<TransportBelt>(
            "transport-belt",
            connectToNetwork = true,
        ) {
            direction = Direction.East
            control_behavior = ControlBehavior(
                circuit_enable_disable = true,
                circuit_condition = CircuitCondition(
                    comparator = CompareOperation.Equal,
                    constant = 1
                ),
                circuit_read_hand_contents = true,
                circuit_contents_read_mode = TransportBeltContentReadMode.Hold,
            )
        }
        assertEquals(Direction.East, belt.direction)
        assertEquals(true, belt.controlBehavior.enableDisable)
        assertEquals(TransportBeltContentReadMode.Hold, belt.controlBehavior.readContentsMode)
    }

    @Test
    fun `can load underground belt`() {
        testSaveLoad<UndergroundBelt>("underground-belt") {
            type = IOType.Input
        }
        testSaveLoad<UndergroundBelt>("underground-belt") {
            type = IOType.Output
        }
    }

    @Test
    fun `can load splitter`() {
        testSaveLoad<Splitter>("splitter")
        testSaveLoad<Splitter>("splitter") {
            input_priority = SplitterPriority.Left
            output_priority = SplitterPriority.Right
            filter = "iron-plate"
        }
    }

    @Test
    fun `can load loader`() {
        testSaveLoad<Loader>("loader") {
            type = IOType.Input
        }
        testSaveLoad<Loader>("loader") {
            type = IOType.Output
            filters = itemFilterList("iron-plate", "copper-plate")
        }
    }

    @Test
    fun `can load linked belt`() {
        testSaveLoad<LinkedBelt>("linked-belt") {
            type = IOType.Output
        }
        testSaveLoad<LinkedBelt>("linked-belt") {
            type = IOType.Input
            link_id = 4
        }
    }
}
