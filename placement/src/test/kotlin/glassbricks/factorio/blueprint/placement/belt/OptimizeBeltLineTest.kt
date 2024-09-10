package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.beltcp.GridCp
import glassbricks.factorio.blueprint.placement.beltcp.addBeltPlacements
import glassbricks.factorio.blueprint.placement.shifted
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
        additionalCost: (Int, OptionalEntityPlacement<*>) -> Double = { distance, _ ->
            distance / 100.0
        },
    ): String {
        val (entities, line) = createBeltLine(
            inStr,
            startPos,
            direction,
        )
        fun createModel(): Pair<GridCp, EntityPlacementModel> {
            val beltGrid = BeltGrid()
            beltGrid.addBeltLine(line)

            val model = EntityPlacementModel()
            val belts = model.addBeltPlacements(beltGrid)
            model.addFixedEntities(entities)

            for (placement in model.placements) {
                if (placement !is OptionalEntityPlacement) continue
                if (placement.prototype is UndergroundBeltPrototype) {
                    placement.cost = ugRelCost
                }
                val distance = placement.position.occupiedTile().manhattanDistanceTo(startPos)
                placement.cost += additionalCost(distance, placement)
            }
            return belts to model
        }

        fun solveModel(model: EntityPlacementModel): String {
            val solution = model.solve()
            assertEquals(CpSolverStatus.OPTIMAL, solution.status)

            val resultEntities = solution.export()
            return getBeltsAsStr(resultEntities, startPos, direction, inStr.length)
        }

        fun solveUsingCp(): String {
            val (belts, model) = createModel()
            return solveModel(model)
        }

        fun initialSolutionOnly(): String {
            val (belts, model) = createModel()
            val initialSolution = belts.solveInitialSolution()
            assertEquals(1, initialSolution.size)
            return getBeltPlacementsAsStr(initialSolution.single().curSolution!!, inStr.length)
        }

        val solA = solveUsingCp()
//        val solB = initialSolutionOnly()
//        val solANoWalls = solA.replace("#", " ")
//        assertEquals(solANoWalls, solB, "Solutions differ")
        return solA
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
        assertEquals("><><><><", result)
    }

    @Test
    fun `chooses ug with least cost`() {
        val result = testBeltLine(">#   <>#<", ugRelCost = 2.3, additionalCost = { distance, placement ->
            if (distance == 4 && placement.originalEntity.let {
                    it is UndergroundBelt && it.ioType == IOType.Output
                }) -0.2
            else 0.0
        })
        assertEquals(">#  <> #<", result)

    }
}
