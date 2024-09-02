package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.ops.addBeltLinesFrom
import glassbricks.factorio.blueprint.placement.toBlueprintEntities
import glassbricks.factorio.blueprint.prototypes.TransportBeltConnectablePrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OptimizeBeltsTest {
    fun testOptimizeBelts(inStr: String, ugRelCost: Double): String {
        val startPos = tilePos(0, 0)
        val entities = createEntities(inStr, startPos)

        val model = EntityPlacementModel()
        model.addBeltLinesFrom(entities)
        model.addFixedEntities(entities.filter<BlueprintEntity> { it.prototype !is TransportBeltConnectablePrototype })
        for (placement in model.placements) {
            if (placement.prototype is UndergroundBeltPrototype && placement is OptionalEntityPlacement) {
                placement.cost = ugRelCost
            }
        }

        val solution = model.solve()
        assertEquals(CpSolverStatus.OPTIMAL, solution.status)

        val resultEntities = solution.toBlueprintEntities(null)
        return getBeltsAsStr(resultEntities, startPos, CardinalDirection.East, inStr.length)
    }

    @Test
    fun `replacing ug with normal belt`() {
        val result = testOptimizeBelts(" >  < ", ugRelCost = 2.3)
        assertEquals(" ==== ", result)
    }

    @Test
    fun `replacing belt with ug`() {
        val result = testOptimizeBelts(" ===== ", ugRelCost = 2.3)
        assertEquals(" >   < ", result)
    }

    @Test
    fun `better handling over obstacle`() {
        val result = testOptimizeBelts("=>#<>#<==", ugRelCost = 2.3)
        assertEquals("=>#  #<==", result)
    }

    @Test
    fun `with isolated input ug belt`() {
        val result = testOptimizeBelts("<=>", ugRelCost = 2.3)
        assertEquals("<=>", result)
    }
}
