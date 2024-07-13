package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.json.InfinityFilter
import kotlin.test.Test

class ContainerKtTest {
    @Test
    fun `can load simple container`() {
        testSaveLoad<Container>("iron-chest")
        testSaveLoad<Container>("iron-chest") {
            bar = 1
        }
    }

    @Test
    fun `can load logistic containers`() {
        testSaveLoad<LogisticContainer>("logistic-chest-passive-provider")
        testSaveLoad<LogisticContainer>("logistic-chest-storage") {
            request_filters = listOf(LogisticFilter(name = "iron-plate", count = 1, index = 1))
        }
        testSaveLoad<LogisticContainer>("logistic-chest-requester") {
            request_filters = listOf(
                LogisticFilter(name = "iron-plate", count = 1, index = 1),
                LogisticFilter(name = "copper-plate", count = 2, index = 5)
            )
            request_from_buffers = true
        }
        testSaveLoad<LogisticContainer>("logistic-chest-requester") {
            control_behavior = ControlBehaviorJson(
                circuit_mode_of_operation = LogisticContainerModeOfOperation.SetRequests.asMode()
            )
        }
    }

    @Test
    fun `can load infinity containers`() {
        testSaveLoad<InfinityContainer>("infinity-chest") {
            infinity_settings = InfinitySettings(
                filters = listOf(
                    InfinityFilter(
                        name = "iron-plate",
                        count = 1,
                        mode = InfinityFilterMode.AtLeast,
                        index = 1
                    )
                ),
                remove_unfiltered_items = true,
            )
        }
    }
}
