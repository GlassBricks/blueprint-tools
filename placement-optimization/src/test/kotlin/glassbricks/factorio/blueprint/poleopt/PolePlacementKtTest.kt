package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.entity.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.placement.poles.PolePlacement
import glassbricks.factorio.blueprint.placement.poles.PoleCoverProblem
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.Test
import kotlin.test.assertEquals

class PolePlacementKtTest {
    @Test
    fun `creating candidate poles`() {
        val entities = DefaultSpatialDataStructure<Entity>()
        val entityList = listOf(
            powerable(tilePos(0, 0)),
            powerable(tilePos(1, 0)),
            powerable(tilePos(3, 0)),
            nonPowerable(tilePos(4, 0)),
            powerable(tilePos(6, 0)),
            powerable(tilePos(-40, 0))
        )
        entities.addAll(entityList)
        val locations = listOf(
            tilePos(2, 1),
            tilePos(6, 0),
            tilePos(20, 1),
        ).map {
            PolePlacement(smallPole, smallPole.tileSnappedPosition(it))
        }
        val candidatePoles = PoleCoverProblem(entities, DefaultSpatialDataStructure(locations))

        assertEquals(3, candidatePoles.candidatePoles.size)

        val pole2 = candidatePoles.candidatePoles.getInTile(tilePos(2, 1)).single()
        assertEquals(setOf(entityList[0], entityList[1], entityList[2]),
            candidatePoles.coveredEntities[pole2]!!.toSet())

        val pole5 = candidatePoles.candidatePoles.getInTile(tilePos(6, 0)).single()
        assertEquals(setOf(entityList[4]), candidatePoles.coveredEntities[pole5]!!.toSet())

        val pole20 = candidatePoles.candidatePoles.getInTile(tilePos(20, 1)).single()
        assertEquals(emptySet(), candidatePoles.coveredEntities[pole20]!!.toSet())

//        assertEquals(setOf(pole2), candidatePoles.poweredBy[entityList[0]])
//        assertEquals(setOf(pole2), candidatePoles.poweredBy[entityList[1]])
//        assertEquals(setOf(pole2), candidatePoles.poweredBy[entityList[2]])
//        assertEquals(null, candidatePoles.poweredBy[entityList[3]])
//        assertEquals(setOf(pole5), candidatePoles.poweredBy[entityList[4]])
//        assertEquals(null, candidatePoles.poweredBy[entityList[5]])

        // connections

        assertEquals(setOf(pole5), candidatePoles.computeNeighbors(pole2).toSet())
        assertEquals(setOf(pole2), candidatePoles.computeNeighbors(pole5).toSet())

        assertEquals(emptySet(), candidatePoles.computeNeighbors(pole20).toSet())
    }
}
