package glassbricks.factorio.blueprint.placement.belts

import com.google.ortools.Loader
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.CpSolver
import com.google.ortools.sat.CpSolverStatus
import glassbricks.factorio.blueprint.tileBbox
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BeltGridTest {

    @Test
    fun testStraightLine() {
        Loader.loadNativeLibraries()
        val cp = CpModel()
        val tiles = tileBbox(0, 0, 5, 5)
        val grid = BeltGrid(cp, tiles)
        grid.getTile(0, 0)!!.beltType = BeltTileType.FixedInputBelt
        grid.getTile(4, 4)!!.beltType = BeltTileType.FixedOutputSpot

        val solver = CpSolver()
        solver.parameters.maxTimeInSeconds = 5.0
        val result = solver.solve(cp)
        assertEquals(CpSolverStatus.OPTIMAL, result)
    }
}
