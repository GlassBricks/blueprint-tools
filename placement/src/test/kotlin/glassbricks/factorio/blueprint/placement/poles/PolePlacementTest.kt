package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
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
        val entityList = model.addFixedEntities(
            listOf<Entity<*>>(
                powerable(tilePos(0, 0)),
                powerable(tilePos(1, 0)),
                powerable(tilePos(3, 0)),
                nonPowerable(tilePos(4, 0)),
                powerable(tilePos(6, 1)),
                powerable(tilePos(-40, 0))
            )
        )
        listOf(
            tilePos(2, 1),
            tilePos(6, 0),
            tilePos(20, 1),
        ).map {
            model.addPlacement(smallPole, smallPole.tileSnappedPosition(it))
        }
        val polePlacements = PolePlacements(model, PolePlacementOptions(addToModel = false))
        assertEquals(3, polePlacements.poles.size)

        val pole2 = model.placements.getInTile(tilePos(2, 1)).single() as PolePlacement
        assertEquals(
            setOf(entityList[0], entityList[1], entityList[2]),
            polePlacements.coveredEntities[pole2]!!.toSet()
        )

        val pole5 = model.placements.getInTile(tilePos(6, 0)).single() as PolePlacement
        assertEquals(setOf(entityList[4]), polePlacements.coveredEntities[pole5]!!.toSet())

        val pole20 = model.placements.getInTile(tilePos(20, 1)).single()
        assertEquals(emptySet(), polePlacements.coveredEntities[pole20]!!.toSet())

        assertEquals(listOf(pole2), polePlacements.poweringPoles[entityList[0]])
        assertEquals(listOf(pole2), polePlacements.poweringPoles[entityList[1]])
        assertEquals(listOf(pole2), polePlacements.poweringPoles[entityList[2]])
        assertEquals(null, polePlacements.poweringPoles[entityList[3]])
        assertEquals(listOf(pole5), polePlacements.poweringPoles[entityList[4]])
        assertEquals(null, polePlacements.poweringPoles[entityList[5]])

        // connections

        assertEquals(listOf(pole5), polePlacements.neighborsMap[pole2])
        assertEquals(listOf(pole2), polePlacements.neighborsMap[pole5])

        assertEquals(emptyList(), polePlacements.neighborsMap[pole20])
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
        val polePlacements = model.addPolePlacements(
            polesToAdd = listOf(smallPole),
            bounds = entities.enclosingTileBox(),
            options = PolePlacementOptions(removeEmptyPolesReach1 = true)
        )
        assertEquals(9, model.placements.size)
        model.timeLimitInSeconds = 1.0
        val solution = model.solve()

        assertEquals(2.0, solution.solver.objectiveValue())
        val usedPoles = solution.getSelectedOptionalEntities()
        assertEquals(2, usedPoles.size)

        for (entity in fixedPlacements) {
            if (entity.prototype.usesElectricity) {
                val poweringPoles = polePlacements.poweringPoles[entity]!!
                // compiler bug?
                @Suppress("RemoveExplicitTypeArguments")
                val usedPole = poweringPoles.firstOrNull { usedPoles.contains<EntityPlacement<*>>(it) }
                assertNotNull(usedPole)
                assertTrue(entity in polePlacements.coveredEntities[usedPole].orEmpty())
            }
        }
    }
}
