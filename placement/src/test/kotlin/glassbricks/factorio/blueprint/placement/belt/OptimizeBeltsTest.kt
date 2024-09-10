package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.PlacementSolution
import glassbricks.factorio.blueprint.placement.beltcp.addBeltLinesFrom
import glassbricks.factorio.blueprint.prototypes.TransportBeltConnectablePrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OptimizeBeltsTest {
    fun testOptimizeBelts(inStr: String, ugRelCost: Double): String {
        val startPos = tilePos(0, 0)
        val entities = createEntities(inStr, startPos)

        val solution = runOptimize(entities, ugRelCost)

        val resultEntities = solution.export()
        return getBeltsAsStr(resultEntities, startPos, CardinalDirection.East, inStr.length)
    }

    private fun runOptimize(
        entities: MutableSpatialDataStructure<BlueprintEntity>,
        ugRelCost: Double,
    ): PlacementSolution {
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
        return solution
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
    fun `with isolated ug belts`() {
        val result = testOptimizeBelts("<=>", ugRelCost = 2.3)
        assertEquals("<=>", result)
    }

    @Test
    fun `dodging T intersection`() {
        val line1 = createEntities("=", tilePos(0, 1), Direction.North)
        val line2 = createEntities("> <", tilePos(-1, 0), Direction.East)
        line1.addAll(line2)

        val solution = runOptimize(line1, ugRelCost = 50.0)
        val resultEntities = solution.export()
        val entityCount = resultEntities.groupingBy { it.prototype.name }.eachCount()
        assertEquals(1, entityCount["transport-belt"])
        assertEquals(2, entityCount["underground-belt"])
    }

    @Test
    fun `actual T intersection`() {
        val line1 = createEntities("=", tilePos(0, 1), Direction.North)
        val line2 = createEntities("===", tilePos(-1, 0), Direction.East)
        line1.addAll(line2)

        val solution = runOptimize(line1, ugRelCost = 2.3)
        val resultEntities = solution.export()
        val entityCount = resultEntities.groupingBy { it.prototype.name }.eachCount()
        assertEquals(4, entityCount["transport-belt"])
    }

    @Test
    fun `overlapping lines`() {
        val line1 = createEntities(">  <", tilePos(0, 0), Direction.East)
        val line2 = createEntities(" == ", tilePos(0, 0), Direction.East)
        line1.addAll(line2)

        val solution = runOptimize(line1, ugRelCost = 2.3)
        val resultEntities = solution.export()
        val entityCount = resultEntities.groupingBy { it.prototype.name }.eachCount()
        assertEquals(2, entityCount["transport-belt"])
        assertEquals(2, entityCount["underground-belt"])
    }
}
