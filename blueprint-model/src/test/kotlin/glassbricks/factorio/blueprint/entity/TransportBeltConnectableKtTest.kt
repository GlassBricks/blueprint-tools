package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.json.ControlBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class TransportBeltConnectableKtTest {
    @Test
    fun `can load transport belt`() {
        testSaveLoad(TransportBelt::class, "transport-belt")
        testSaveLoad(TransportBelt::class, "transport-belt", connectToNetwork = true) {
            control_behavior = ControlBehavior(
                circuit_enable_disable = false
            )
        }
        val belt = testSaveLoad(TransportBelt::class, "transport-belt",
            connectToNetwork = true,
            build = fun EntityJson.() {
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
            })
        assertEquals(Direction.East, belt.direction)
        assertEquals(true, belt.controlBehavior.enableDisable)
        assertEquals(TransportBeltContentReadMode.Hold, belt.controlBehavior.readContentsMode)
    }

    @Test
    fun `can load underground belt`() {
        testSaveLoad(UndergroundBelt::class, "underground-belt", null, false) {
            type = IOType.Input
        }
        testSaveLoad(UndergroundBelt::class, "underground-belt", null, false) {
            type = IOType.Output
        }
    }

    @Test
    fun `can load splitter`() {
        testSaveLoad(Splitter::class, "splitter")
        testSaveLoad(Splitter::class, "splitter", null, false) {
            input_priority = SplitterPriority.Left
            output_priority = SplitterPriority.Right
            filter = "iron-plate"
        }
    }

    @Test
    fun `can load loader`() {
        testSaveLoad(Loader::class, "loader", null, false) {
            type = IOType.Input
        }
        testSaveLoad(Loader::class, "loader", null, false) {
            type = IOType.Output
            filters = itemFilterList("iron-plate", "copper-plate")
        }
    }

    @Test
    fun `can load linked belt`() {
        testSaveLoad(LinkedBelt::class, "linked-belt", null, false) {
            type = IOType.Output
        }
        testSaveLoad(LinkedBelt::class, "linked-belt", null, false) {
            type = IOType.Input
            link_id = 4
        }
    }
}
