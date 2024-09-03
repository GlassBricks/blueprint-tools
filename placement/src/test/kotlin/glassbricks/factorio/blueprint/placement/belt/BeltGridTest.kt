package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.Loader
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.CpSolver
import com.google.ortools.sat.CpSolverStatus
import com.google.ortools.sat.LinearExpr
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.intLinearExpr
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class BeltGridTest {
    val grid = GridConfig()

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

    private fun entityCostSolve(): Pair<Grid, CpSolver> {
        val vars = grid.applyTo(CpModel())
        addEntityCost(vars)
        val solver = CpSolver()
        solver.parameters.apply {
            logSearchProgress = true
            logToStdout = true
        }
        assertEquals(CpSolverStatus.OPTIMAL, solver.solve(vars.cp))
        return vars to solver
    }

    private fun materialCostSolve(): Pair<Grid, CpSolver> {
        val vars = grid.applyTo(CpModel())
        addMaterialCost(vars)
        val solver = CpSolver()
        assertEquals(CpSolverStatus.OPTIMAL, solver.solve(vars.cp))
        return vars to solver
    }

    private fun addEntityCost(vars: Grid) {
        val cost = LinearExpr.sum(
            vars.belts.values
                .flatMapTo(mutableSetOf()) {
                    it.selectedBelt.values.flatMap { it.values }
                }
                .toTypedArray()
        )
        vars.cp.minimize(cost)
    }

    private fun addMaterialCost(vars: Grid) {
        val costs = mapOf(
            normalBelt.beltProto to 1,
            normalBelt.ugProto to 4,
            fastTier.beltProto to 10,
            fastTier.ugProto to 50,
        )
        val cost =
            vars.belts.values
                .flatMap { it.selectedBelt.values }
                .flatMap {
                    it.map {
                        it.value to it.key.prototype.let { costs[it]!! }
                    }
                }
                .toMap()
        val result = intLinearExpr(cost)
        vars.cp.minimize(result)
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
        val (vars, solver) = materialCostSolve()
        assertEquals(12.0, solver.objectiveValue())
        for (i in 0..<12) {
            val tile = start.shifted(direction, i)
            val belt = vars[tile]!!.selectedBelt[direction]!!
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            assertTrue(solver.booleanValue(belt[normalBelt.belt]!!))
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
        val (vars, solver) = entityCostSolve()

        assertEquals(2.0, solver.objectiveValue())
        assertTrue(vars[start]!!.selectedBelt[direction]!![normalBelt.inputUg]!!
            .let { solver.booleanValue(it) })
        assertTrue(vars[start.shifted(direction, 2)]!!.selectedBelt[direction]!![normalBelt.outputUg]!!
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

        val (vars, solver) = entityCostSolve()

        assertEquals(4.0, solver.objectiveValue())
        for (dist in listOf(0, 6)) {
            val tile = start.shifted(direction, dist)
            val ugVar = vars[tile]!!.selectedBelt[direction]!![normalBelt.inputUg]!!
            assertTrue(solver.booleanValue(ugVar))
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            val tile5 = start.shifted(direction, dist + 5)
            val ugVar5 = vars[tile5]!!.selectedBelt[direction]!![normalBelt.outputUg]!!
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
        val (vars, solver) = materialCostSolve()
        assertEquals((4 + 2 * 4).toDouble(), solver.objectiveValue())
        // along first belt, should be all belt
        for (i in 0..<4) {
            val tile = tilePos(i - 2, 0)
            val belt = vars[tile]!!.selectedBelt[CardinalDirection.East]!!
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            assertTrue(solver.booleanValue(belt[normalBelt.belt]!!))
        }
        val tile1 = tilePos(0, -2)
        val belt1 = vars[tile1]!!.selectedBelt[CardinalDirection.South]!!
        assertEquals(2, solver.value(vars[tile1]!!.lineId))
        assertTrue(solver.booleanValue(belt1[normalBelt.inputUg]!!))
        val tile2 = tilePos(0, -2 + 4)
        val belt2 = vars[tile2]!!.selectedBelt[CardinalDirection.South]!!
        assertEquals(2, solver.value(vars[tile2]!!.lineId))
        assertTrue(solver.booleanValue(belt2[normalBelt.outputUg]!!))
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
        val (vars, solver) = entityCostSolve()
        assertEquals(10.0, solver.objectiveValue())
        for (dist in listOf(0, 8, 16)) {
            val tile = start.shifted(direction, dist)
            assertTrue(solver.booleanValue(vars[tile]!!.selectedBelt[direction]!![fastTier.inputUg]!!))
            assertEquals(1, solver.value(vars[tile]!!.lineId))
            val tile7 = start.shifted(direction, dist + 7)
            assertTrue(solver.booleanValue(vars[tile7]!!.selectedBelt[direction]!![fastTier.outputUg]!!))
            assertEquals(1, solver.value(vars[tile7]!!.lineId))
        }
        for (dist in listOf(6, 12)) {
            val tile = start.shifted(direction, dist)
            assertTrue(solver.booleanValue(vars[tile]!!.selectedBelt[direction]!![normalBelt.inputUg]!!))
            assertEquals(2, solver.value(vars[tile]!!.lineId))
            val tile5 = start.shifted(direction, dist + 5)
            assertTrue(solver.booleanValue(vars[tile5]!!.selectedBelt[direction]!![normalBelt.outputUg]!!))
            assertEquals(2, solver.value(vars[tile5]!!.lineId))
        }
    }
}
