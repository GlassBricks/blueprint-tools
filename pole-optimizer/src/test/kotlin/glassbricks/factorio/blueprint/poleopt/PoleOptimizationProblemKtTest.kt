package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class PoleOptimizationProblemTest {
    @Test
    fun `test simple poleILP`() {
        val entities = DefaultSpatialDataStructure<Entity>()
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
        val candidatePoles = getCompleteCandidatePoleSet(
            entities, listOf(smallPole),
            bounds = entities.enclosingBox()
        )
        assertEquals(3, candidatePoles.poles.size)
        val problem = createDefaultPoleILP(candidatePoles)
        val solver = problem.solver
        solver.setTimeLimit(1000)
        solver.solve()

        assertEquals(2.0, solver.objective().value())
        val usedPoles = problem.getSelectedPoles()
        assertEquals(2, usedPoles.size)

        for (entity in entities) {
            val pole = problem.getPoweringPole(entity)
            if (entity.prototype.usesElectricity) {
                assertNotNull(pole)
                assertTrue(pole in usedPoles)
                assertTrue(entity in problem.poleSet.getPoweredEntities(pole))
            } else {
                assertEquals(null, pole)
            }
        }
    }
}
