package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.Test
import kotlin.test.assertEquals

class CandidatePoleKtTest {
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
            CandidatePole(smallPole, smallPole.tileSnappedPosition(it))
        }
        val candidatePoles = CandidatePoleSet(entities, DefaultSpatialDataStructure(locations))

        assertEquals(3, candidatePoles.poles.size)

        val pole2 = candidatePoles.poles.getInTile(tilePos(2, 1)).single()
        assertEquals(setOf(entityList[0], entityList[1], entityList[2]),
            candidatePoles.getPoweredEntities(pole2).toSet())

        val pole5 = candidatePoles.poles.getInTile(tilePos(6, 0)).single()
        assertEquals(setOf(entityList[4]), candidatePoles.getPoweredEntities(pole5).toSet())

        val pole20 = candidatePoles.poles.getInTile(tilePos(20, 1)).single()
        assertEquals(emptySet(), candidatePoles.getPoweredEntities(pole20).toSet())

//        assertEquals(setOf(pole2), candidatePoles.poweredBy[entityList[0]])
//        assertEquals(setOf(pole2), candidatePoles.poweredBy[entityList[1]])
//        assertEquals(setOf(pole2), candidatePoles.poweredBy[entityList[2]])
//        assertEquals(null, candidatePoles.poweredBy[entityList[3]])
//        assertEquals(setOf(pole5), candidatePoles.poweredBy[entityList[4]])
//        assertEquals(null, candidatePoles.poweredBy[entityList[5]])

        // connections

        assertEquals(setOf(pole5), candidatePoles.getNeighbors(pole2).toSet())
        assertEquals(setOf(pole2), candidatePoles.getNeighbors(pole5).toSet())

        assertEquals(emptySet(), candidatePoles.getNeighbors(pole20).toSet())
    }
}
