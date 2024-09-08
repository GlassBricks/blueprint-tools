package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.Loader
import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.get
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class BeltGridTest {
    val grid = BeltPlacementConfig()

    companion object {
        val params = listOf(
            tilePos(2, 5) to CardinalDirection.North,
            tilePos(-1, 3) to CardinalDirection.East,
            tilePos(0, 0) to CardinalDirection.South,
            tilePos(7, 2) to CardinalDirection.West,
        )

        init {
            Loader.loadNativeLibraries()
        }
    }

    private fun entityCostSolve(): BeltPlacements {
        val vars = grid.addTo(EntityPlacementModel())
        assertEquals(CpSolverStatus.OPTIMAL, vars.model.solve().status)
        return vars
    }

    private fun materialCostSolve(): BeltPlacements {
        val vars = grid.addTo(EntityPlacementModel())
        addMaterialCost(vars)
        assertEquals(CpSolverStatus.OPTIMAL, vars.model.solve().status)
        return vars
    }

    private fun addMaterialCost(vars: BeltPlacements) {
        val costs = mapOf(
            normalBelt.beltProto to 1,
            normalBelt.ugProto to 4,
            fastTier.beltProto to 10,
            fastTier.ugProto to 50,
        )
        for (placement in vars.model.placements) if (placement is OptionalEntityPlacement<*>) {
            placement.cost = costs[placement.prototype]!!.toDouble()
        }
    }

    @ParameterizedTest
    @FieldSource("params")
    fun `all belt`(params: Pair<TilePosition, CardinalDirection>) {
        val (start, direction) = params
        grid.addBeltLine(
            start = start,
            direction,
            length = 12,
            beltTiers = setOf(normalBelt, fastTier)
        )
        val vars = materialCostSolve()
        val solver = vars.model.solver
        assertEquals(12.0, solver.objectiveValue())
        for (i in 0..<12) {
            val tile = start.shifted(direction, i)
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            val belt = vars[tile]!!.selectedBelt[direction, normalBelt.belt]!!
            assertTrue(solver.booleanValue(belt.selected))
        }
    }

    @ParameterizedTest
    @FieldSource("params")
    fun `a single ug`(params: Pair<TilePosition, CardinalDirection>) {
        val (start, direction) = params
        val beltOptions = List(3) { idx ->
            BeltLineTile(
                mustBeNotEmpty = false,
                allowedBeltTiers = if (idx == 1) emptySet() else setOf(normalBelt),
                mustMatch = null
            )
        }
        grid.addBeltLine(BeltLine(start, direction, beltOptions))
        val vars = entityCostSolve()
        val solver = vars.model.solver
        assertEquals(2.0, solver.objectiveValue())
        assertTrue(
            vars[start]!!.selectedBelt[direction, normalBelt.inputUg]!!.selected
                .let { solver.booleanValue(it) })
        assertTrue(
            vars[start.shifted(direction, 2)]!!.selectedBelt[direction, normalBelt.outputUg]!!.selected
                .let { solver.booleanValue(it) })
    }

    @ParameterizedTest
    @FieldSource("params")
    fun `ug chain`(params: Pair<TilePosition, CardinalDirection>) {
        val (start, direction) = params
        grid.addBeltLine(
            start = start,
            direction,
            length = 12,
            beltTiers = setOf(normalBelt)
        )

        val vars = entityCostSolve()
        val solver = vars.model.solver

        assertEquals(4.0, solver.objectiveValue())
        for (dist in listOf(0, 6)) {
            val tile = start.shifted(direction, dist)
            val ugVar = vars[tile]!!.selectedBelt[direction, normalBelt.inputUg]!!.selected
            assertTrue(solver.booleanValue(ugVar))
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            val tile5 = start.shifted(direction, dist + 5)
            val ugVar5 = vars[tile5]!!.selectedBelt[direction, normalBelt.outputUg]!!.selected
            assertTrue(solver.booleanValue(ugVar5))
            assertEquals(1, solver.value(vars[tile5]!!.lineId))
        }
    }

    @Test
    fun intersection() {
        grid.addBeltLine(
            start = tilePos(-2, 0),
            direction = CardinalDirection.East,
            length = 4,
            beltTiers = setOf(normalBelt)
        )
        grid.addBeltLine(
            start = tilePos(0, -2),
            direction = CardinalDirection.South,
            length = 5,
            beltTiers = setOf(normalBelt)
        )
        val vars = materialCostSolve()
        val solver = vars.model.solver
        assertEquals((4 + 2 * 4).toDouble(), solver.objectiveValue())
        // along first belt, should be all belt
        for (i in 0..<4) {
            val tile = tilePos(i - 2, 0)
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            val belt = vars[tile]!!.selectedBelt[CardinalDirection.East, normalBelt.belt]!!.selected
            assertTrue(solver.booleanValue(belt))
        }
        val tile1 = tilePos(0, -2)
        val belt1 = vars[tile1]!!.selectedBelt[CardinalDirection.South, normalBelt.inputUg]!!.selected
        assertEquals(2, solver.value(vars[tile1]!!.lineId))
        assertTrue(solver.booleanValue(belt1))
        val tile2 = tilePos(0, -2 + 4)
        val belt2 = vars[tile2]!!.selectedBelt[CardinalDirection.South, normalBelt.outputUg]!!.selected
        assertEquals(2, solver.value(vars[tile2]!!.lineId))
        assertTrue(solver.booleanValue(belt2))
    }

    @ParameterizedTest
    @FieldSource("params")
    fun `underground weaving`(params: Pair<TilePosition, CardinalDirection>) {
        val (start, direction) = params
        // fast:   >......<>......<>......<
        // normal: ......>....<>....<......
        grid.addBeltLine(
            start = start,
            direction,
            length = 8 * 3,
            beltTiers = setOf(fastTier)
        )
        grid.addBeltLine(
            start = start.shifted(direction, 6),
            direction,
            length = 6 * 2,
            beltTiers = setOf(normalBelt)
        )
        val vars = entityCostSolve()
        val solver = vars.model.solver
        assertEquals(10.0, solver.objectiveValue())
        for (dist in listOf(0, 8, 16)) {
            val tile = start.shifted(direction, dist)
            assertTrue(solver.booleanValue(vars[tile]!!.selectedBelt[direction, fastTier.inputUg]!!.selected))
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            val tile7 = start.shifted(direction, dist + 7)
            assertTrue(solver.booleanValue(vars[tile7]!!.selectedBelt[direction, fastTier.outputUg]!!.selected))
            assertEquals(1, solver.value(vars[tile7]!!.lineId))
        }
        for (dist in listOf(6, 12)) {
            val tile = start.shifted(direction, dist)
            assertTrue(solver.booleanValue(vars[tile]!!.selectedBelt[direction, normalBelt.inputUg]!!.selected))
            assertEquals(2, solver.value(vars[tile]!!.lineId))
            val tile5 = start.shifted(direction, dist + 5)
            assertTrue(solver.booleanValue(vars[tile5]!!.selectedBelt[direction, normalBelt.outputUg]!!.selected))
            assertEquals(2, solver.value(vars[tile5]!!.lineId))
        }
    }
}
