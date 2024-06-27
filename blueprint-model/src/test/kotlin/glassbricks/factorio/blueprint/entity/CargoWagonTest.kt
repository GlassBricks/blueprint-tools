package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RollingStockKtTest {
    @Test
    fun `can load a cargo wagon`() {
        val wagon = loadEntity("cargo-wagon") {
            orientation = 0.5
            inventory = Inventory(
                bar = 1,
                filters = listOf(
                    ItemFilter("iron-plate", 1),
                    ItemFilter("copper-plate", 5),
                )
            )
        }
        assertTrue(wagon is CargoWagon)
        assertEquals(wagon.orientation, 0.5)
        assertEquals(wagon.bar, 1)
        assertEquals(
            wagon.getFiltersAsList(), listOf(
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
    fun `can load a locomotive`() {
        val (schedule, loco) = setupLocomotive()
        assertEquals(loco.orientation, 0.5)
        assertEquals(loco.color, Color(1.0, 2.0, 3.0))
        assertEquals(loco.schedule, schedule)

    }

    @Test
    fun `can save a locomotive`() {
        val (_, loco) = setupLocomotive()
        val blueprint: BlueprintJson = BlueprintJson(icons = listOf())
        loco.orientation = 0.25
        loco.color = Color(3.0, 2.0, 1.0)
        val schedule2 = listOf(
            ScheduleRecord("unload2", listOf(WaitCondition(WaitConditionType.Empty))),
            ScheduleRecord("load", listOf(WaitCondition(WaitConditionType.Full))),
        )
        loco.schedule = schedule2
        val entityJson = loco.toJsonIsolated(EntityNumber(1))
        assertEquals(entityJson.name, "locomotive")
        assertEquals(entityJson.orientation, 0.25)
        assertEquals(entityJson.color, Color(3.0, 2.0, 1.0))
        blueprint.schedules = null
        blueprint.setEntitiesFrom(listOf(loco, loco.copy()))
        assertEquals(
            blueprint.schedules, listOf(
                Schedule(
                    locomotives = listOf(
                        EntityNumber(1),
                        EntityNumber(2)
                    ), schedule = schedule2
                )
            )
        )
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
        val loco = loadEntity("locomotive", blueprint) {
            entity_number = EntityNumber(1)
            orientation = 0.5
            color = Color(1.0, 2.0, 3.0)
        }
        assertTrue(loco is Locomotive)
        return Pair(schedule, loco)
    }

}
