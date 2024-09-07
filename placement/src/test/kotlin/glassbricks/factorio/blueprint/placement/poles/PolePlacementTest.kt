package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PolePlacementTest {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test pole placement`() {
        val model = EntityPlacementModel()
        val addedEntities = model.addFixedEntities(
            listOf<Entity<*>>(
                powerable(tilePos(0, 0)),
                powerable(tilePos(1, 0)),
                powerable(tilePos(3, 0)),
                nonPowerable(tilePos(4, 0)),
                powerable(tilePos(6, 0)),
                powerable(tilePos(8, 0)),
            )
        )
        val polePlacements = model.addPolePlacements(listOf(smallPole))
        assertEquals(3, polePlacements.poles.size)
        val poles = polePlacements.poles.associateBy { it.position.x.toInt() }

        val entities = List(9) { i -> model.placements.find { it.position.x.toInt() == i }!! }

        val pole2 = poles[2]!!
        val pole5 = poles[5]!!
        val pole7 = poles[7]!!
        assertEquals(setOf(entities[0], entities[1], entities[3]), pole2.poweredEntities.toSet())
        assertEquals(setOf(pole5, pole7), pole2.neighbors.toSet())
        assertEquals(setOf(entities[3], entities[6]), pole5.poweredEntities.toSet())
        assertEquals(setOf(pole2, pole7), pole5.neighbors.toSet())
        assertEquals(setOf(entities[6], entities[8]), pole7.poweredEntities.toSet())
        assertEquals(setOf(pole5, pole2), pole7.neighbors.toSet())
    }

    @Test
    fun `test simple pole placement problem`() {
        val entities = DefaultSpatialDataStructure<BlueprintEntity>()
        entities.addAll(
            listOf(
                powerable(tilePos(0, 0)),
                powerable(tilePos(1, 0)),
                powerable(tilePos(3, 0)),
                nonPowerable(tilePos(4, 0)),
                powerable(tilePos(6, 0)),
                nonPowerable(tilePos(8, 0))
            )
        )
        val model = EntityPlacementModel()
        val fixedPlacements = model.addFixedEntities(entities)
        val polePlacements = model.addPolePlacements(poles = listOf(smallPole))
        assertEquals(9, model.placements.size)
        model.solver.parameters.maxTimeInSeconds = 1.0
        val solution = model.solve()

        assertEquals(2.0, solution.solver.objectiveValue())
        val usedPoles = solution.getSelectedOptionalEntities()
        assertEquals(2, usedPoles.size)

        for (entity in fixedPlacements) {
            if (entity.prototype.usesElectricity) {
                val poweringPoles = polePlacements.poles.filter { it.poweredEntities.contains(entity) }
                val usedPole = poweringPoles.firstOrNull { it.placement in usedPoles }
                assertNotNull(usedPole)
                assertTrue(entity in usedPole.poweredEntities)
            }
        }
    }
}
