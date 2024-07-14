package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RollingStockKtTest {
    @Test
    fun `can load and save a cargo wagon`() {
        val wagon = testSaveLoad(CargoWagon::class, "cargo-wagon", null, false) {
            orientation = 0.5
            inventory = Inventory(
                bar = 1,
                filters = listOf(
                    ItemFilter("iron-plate", 1),
                    ItemFilter("copper-plate", 5),
                )
            )
        }
        assertEquals(wagon.orientation, 0.5)
        assertEquals(wagon.bar, 1)
        assertEquals(
            wagon.filtersAsIndexList(), listOf(
                ItemFilter("iron-plate", 1),
                ItemFilter("copper-plate", 5),
            )
        )

        wagon.orientation = 0.25
        wagon.bar = null
        wagon.filters[9] = "coal"

        val entityJson = wagon.toJsonIsolated(EntityNumber(1))
        assertEquals(entityJson.name, "cargo-wagon")
        assertEquals(entityJson.inventory?.bar, null)
        assertEquals(
            entityJson.inventory?.filters, listOf(
                ItemFilter("iron-plate", 1),
                ItemFilter("copper-plate", 5),
                ItemFilter("coal", 10),
            )
        )
        assertEquals(entityJson.orientation, 0.25)
    }

    @Test
    fun `can save and load a locomotive`() {
        val (_, loco) = setupLocomotive()
        val blueprint: BlueprintJson = emptyBlueprint()
        loco.orientation = 0.25
        loco.color = Color(3.0, 2.0, 1.0)
        loco.itemRequests = mapOf("iron-plate" to 2)
        val schedule2 = listOf(
            ScheduleRecord("unload2", listOf(WaitCondition(WaitConditionType.Empty))),
            ScheduleRecord("load", listOf(WaitCondition(WaitConditionType.Full))),
        )
        loco.schedule = schedule2
        val entityJson = loco.toJsonIsolated(EntityNumber(1))
        assertEquals(entityJson.name, "locomotive")
        assertEquals(entityJson.orientation, 0.25)
        assertEquals(entityJson.color, Color(3.0, 2.0, 1.0))
        assertEquals(entityJson.items, mapOf("iron-plate" to 2))
        blueprint.schedules = null
        blueprint.setEntitiesFrom(listOf(loco))
        assertEquals(
            blueprint.schedules, listOf(
                Schedule(
                    locomotives = listOf(EntityNumber(1)),
                    schedule = schedule2
                )
            )
        )
    }

    @Test
    fun `copying a locomotive keeps the schedule`() {
        val (_, loco) = setupLocomotive()
        val loco2 = loco.copyIsolated() as Locomotive
        assertEquals(loco2.schedule, loco.schedule)
    }

    private fun setupLocomotive(): Pair<List<ScheduleRecord>, Locomotive> {
        val schedule = listOf(
            ScheduleRecord("load", listOf(WaitCondition(WaitConditionType.Full))),
            ScheduleRecord("unload", listOf(WaitCondition(WaitConditionType.Empty))),
        )
        val blueprint = BlueprintJson(
            icons = listOf(),
            schedules = listOf(Schedule(locomotives = listOf(EntityNumber(1)), schedule = schedule))
        )
        val loco = testSaveLoad(Locomotive::class, "locomotive", blueprint, false) {
            entity_number = EntityNumber(1)
            orientation = 0.5
            color = Color(1.0, 2.0, 3.0)
            items = mapOf("coal" to 1)
        }
        return Pair(schedule, loco)
    }

}
