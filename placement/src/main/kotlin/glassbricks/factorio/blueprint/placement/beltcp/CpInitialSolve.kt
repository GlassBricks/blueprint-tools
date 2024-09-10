package glassbricks.factorio.blueprint.placement.beltcp

import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.addLiteralEquality
import glassbricks.factorio.blueprint.placement.addLiteralHint
import glassbricks.factorio.blueprint.placement.belt.BeltLineCosts
import glassbricks.factorio.blueprint.placement.belt.BeltLineSolveParams
import glassbricks.factorio.blueprint.placement.belt.BeltType
import glassbricks.factorio.blueprint.placement.belt.OptBeltLine
import glassbricks.factorio.blueprint.placement.belt.solveBeltLines
import glassbricks.factorio.blueprint.placement.get
import io.github.oshai.kotlinlogging.KotlinLogging


private val logger = KotlinLogging.logger {}

/**
 * Generates a solution using a heuristic method.
 *
 * If [force] is true, the solution is forced to be used. Otherwise, it is used as a hint.
 */
fun GridCp.solveBeltLinesAsInitialSolution(
    canPlace: (BeltType, TilePosition, CardinalDirection) -> Boolean = { _, _, _ -> true },
    force: Boolean = false,
    params: BeltLineSolveParams = BeltLineSolveParams(),
) {
    val lines = solveBeltLinesFromCp(this, canPlace, params)
    addSolutionAsHint(this, lines, force)
}

private fun addSolutionAsHint(grid: GridCp, lines: List<OptBeltLine>, force: Boolean) {
    val usedLiterals = mutableSetOf<Literal>()
    for (line in lines) {
        for ((beltType, index) in line.solution) {
            val pos = line.tiles[index].position
            val literal = grid[pos]
                ?.beltPlacements?.get(line.origLine.direction, beltType)
                ?.selected
            if (literal == null) {
                logger.warn { "Corresponding literal not found: $beltType at $pos" }
                continue
            }
            usedLiterals += literal
        }
    }

    for (tile in grid.tiles.values) {
        for (placement in tile.beltPlacements.values) {
            val use = placement.selected in usedLiterals
            if (force) {
                grid.cp.addLiteralEquality(placement.selected, use)
            } else {
                grid.cp.addLiteralHint(placement.selected, use)
            }
        }
    }
}

fun solveBeltLinesFromCp(
    grid: GridCp,
    canPlace: (BeltType, TilePosition, CardinalDirection) -> Boolean = { _, _, _ -> true },
    params: BeltLineSolveParams = BeltLineSolveParams(),
): List<OptBeltLine> {
    val costs = BeltLineCosts { beltType, pos, direction ->
        val cost = grid[pos]?.beltPlacements?.get(direction, beltType)?.cost ?: Double.POSITIVE_INFINITY
        if (!canPlace(beltType, pos, direction)) Double.POSITIVE_INFINITY else cost
    }
    return solveBeltLines(grid, costs, params)
}
