package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.ops.BeltLine
import glassbricks.factorio.blueprint.placement.ops.BeltLineTile
import glassbricks.factorio.blueprint.placement.ops.addBeltLine
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.placement.toBlueprintEntities
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OptimizeBeltLineTest {

    private fun createBeltLine(
        inStr: String,
        startPos: TilePosition = tilePos(2, 2),
        direction: CardinalDirection = CardinalDirection.East,
    ): Pair<MutableSpatialDataStructure<BlueprintEntity>, BeltLine> {
        val beltTier = BeltTier(belt, ug)
        val entities = DefaultSpatialDataStructure<BlueprintEntity>()

        val tiles = inStr.mapIndexed { index, char ->
            if (char == '#') {
                entities.add(blocker.placedAtTile(startPos.shifted(direction, index)))
            }
            BeltLineTile(
                mustBeNotEmpty = char == 'r',
                mustMatch = null,
                allowedBeltTiers = setOf(beltTier),
            )
        }
        val line = BeltLine(
            start = startPos,
            direction = direction,
            tiles = tiles,
        )
        return entities to line
    }

    private fun testBeltLine(
        inStr: String,
        ugRelCost: Double,
        startPos: TilePosition = tilePos(0, 0),
        direction: CardinalDirection = CardinalDirection.East,
    ): String {
        val (entities, line) = createBeltLine(
            inStr,
            startPos,
            direction,
        )
        val beltGrid = BeltGridConfig()
        beltGrid.addBeltLine(line)

        val model = EntityPlacementModel()
        model.addBeltGrid(beltGrid)
        model.addFixedEntities(entities)

        for (placement in model.placements) {
            if (placement.prototype is UndergroundBeltPrototype && placement is OptionalEntityPlacement) {
                placement.cost = ugRelCost
            }
        }
        val solution = model.solve()
        assertEquals(CpSolverStatus.OPTIMAL, solution.status)

        val resultEntities = solution.toBlueprintEntities(null)
        return getBeltsAsStr(resultEntities, startPos, direction, inStr.length)
    }


    @Test
    fun `if rel cost greater than length, uses belts where possible`() {
        val result = testBeltLine("      ", ugRelCost = 7.0)
        assertEquals("======", result)
    }

    @Test
    fun `uses ug belt if obstacle in the way`() {
        val result = testBeltLine("   #  ", ugRelCost = 7.0)
        assertEquals(">  # <", result)
    }

    @Test
    fun `does not skip tile if it must not be empty`() {
        val result = testBeltLine("  #r  ", ugRelCost = 7.0)
        assertEquals("> #<==", result)
        val result2 = testBeltLine(" r#   ", ugRelCost = 7.0)
        assertEquals("=>#  <", result2)
        val result3 = testBeltLine("  # r ", ugRelCost = 7.0)
        assertEquals("> # <=", result3)
        val result4 = testBeltLine(" r # r ", ugRelCost = 7.0)
        assertEquals("=> # <=", result4)
    }

    @Test
    fun `if rel cost gt 1, uses underground belts`() {
        val result = testBeltLine(" ".repeat(6), ugRelCost = 2.3)
        assertEquals(">    <", result)
    }

    @Test
    fun `uses at least 1 belt if longer than reach`() {
        val result = testBeltLine(" ".repeat(7), ugRelCost = 2.3)
        assertTrue(result == ">    <=" || result == "=>    <")
    }

    @Test
    fun `fully undergrounded`() {
        val result = testBeltLine(" ".repeat(6 * 2), ugRelCost = 2.3)
        assertEquals(">    <>    <", result)
    }

    @Test
    fun `underground spam`() {
        val result = testBeltLine("r".repeat(8), ugRelCost = 0.5)
        assertTrue("<<" !in result)
        assertTrue(">>" !in result)
        assertEquals(4, result.count { it == '<' })
        assertEquals(4, result.count { it == '>' })
    }
}
