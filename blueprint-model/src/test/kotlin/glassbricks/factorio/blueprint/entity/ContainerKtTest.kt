package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.json.InfinityFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ContainerKtTest {
    @Test
    fun `can load simple container`() {
        testSaveLoad(Container::class, "iron-chest")
        testSaveLoad(Container::class, "iron-chest", null, false, fun EntityJson.() {
            bar = 1
        })
    }

    @Test
    fun `can load logistic containers`() {
        val passiveProvider = testSaveLoad(LogisticContainer::class, "logistic-chest-passive-provider")
        assertFalse(passiveProvider.allowsFilters)
        val storage = testSaveLoad(LogisticContainer::class, "logistic-chest-storage", null, false, fun EntityJson.() {
            request_filters = listOf(LogisticFilter(name = "iron-plate", count = 1, index = 1))
        })
        assertFalse(storage.allowsFilters)
        assertEquals(1, storage.requestFilters.size)
        testSaveLoad(LogisticContainer::class, "logistic-chest-requester", null, false, fun EntityJson.() {
            request_filters = listOf(
                LogisticFilter(name = "iron-plate", count = 1, index = 1),
                LogisticFilter(name = "copper-plate", count = 2, index = 5)
            )
            request_from_buffers = true
        })
        testSaveLoad(LogisticContainer::class, "logistic-chest-requester", null, false, fun EntityJson.() {
            control_behavior = ControlBehaviorJson(
                circuit_mode_of_operation = LogisticContainerModeOfOperation.SetRequests.asMode()
            )
        })
    }

    @Test
    fun `can load infinity containers`() {
        testSaveLoad(InfinityContainer::class, "infinity-chest", null, false, fun EntityJson.() {
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
        })
    }
}
