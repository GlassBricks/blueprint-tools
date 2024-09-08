package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.addEquality
import glassbricks.factorio.blueprint.placement.addHint
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

data class InitialSolutionParams(
    val initialIntersectionCost: Double = 2.0,
    val intersectionCostMultiplier: Double = 1.5,
    /** Discount for intersection cost if the original belt is used. */
    val intersectionDiscountMultiplier: Double = 0.5,
    val maxConflictIterations: Int = 100,
    val canUseTile: (TilePosition) -> Boolean = { true },
) {
    init {
        require(initialIntersectionCost > 0)
        require(intersectionCostMultiplier > 1)
        require(intersectionDiscountMultiplier in 0.0..1.0)
        require(maxConflictIterations > 0)
    }
}

/**
 * Generates a solution using a heuristic method.
 *
 * If [force] is true, the solution is forced to be used. Otherwise, it is used as a hint.
 */
fun BeltPlacements.addInitialSolution(
    force: Boolean = false,
    params: InitialSolutionParams = InitialSolutionParams(),
) {
    val lines = findInitialSolution(params)
    addHintFromSolution(lines, force)
    if (!force) model.solver.parameters.apply {
        repairHint = true
    }
}

fun BeltPlacements.findInitialSolution(params: InitialSolutionParams = InitialSolutionParams()): List<OptBeltLine> {
    val lines = getOptBeltLines()
    solveBeltLines(lines, params)
    return lines
}

private fun BeltPlacements.addHintFromSolution(lines: List<OptBeltLine>, force: Boolean) {
    val usedLiterals = mutableSetOf<Literal>()
    for (line in lines) {
        check(line.curSolution != null)
        for ((_, placement) in line.curSolution!!) {
            usedLiterals += placement.selected
        }
    }
    for (literal in usedLiterals) {
        if (force)
            cp.addEquality(literal, true)
        else
            cp.addHint(literal, true)
    }
    for (tile in this.tiles.values) {
        for (placement in tile.selectedBelt.values) {
            if (placement.selected !in usedLiterals) {
                if (force)
                    cp.addEquality(placement.selected, false)
                else
                    cp.addHint(placement.selected, false)
            }
        }
    }
}


private class TileInfo(
    val pos: TilePosition,
    var canPlaceBelt: Boolean,
) {
    var onlyUsableBy: BeltLineId = 0
    var numLines = 0

    var intersectionCost: Double = 0.0

    fun canHaveId(id: BeltLineId): Boolean =
        canPlaceBelt && (onlyUsableBy == 0 || onlyUsableBy == id)

    val usedLines = mutableSetOf<BeltLineId>()
}

internal class OptTile(
    val origTile: BeltLineTile,
    val position: TilePosition,
    val options: List<BeltPlacement>,
    val originalOption: BeltPlacement?,
    val mustNotBeEmpty: Boolean,
)

typealias BeltPlacementSolution = List<Pair<Int, BeltPlacement>>

class OptBeltLine internal constructor(
    val id: BeltLineId,
    val origLine: BeltLine,
    internal val tiles: List<OptTile>,
) {
    internal val ugPrototypes = buildSet {
        for (tile in tiles) for (option in tile.options) {
            if (option.beltType is BeltType.Underground) {
                add(option.beltType.prototype)
            }
        }
    }
    var curSolution: BeltPlacementSolution? = null
        internal set
}

private fun BeltPlacements.getOptBeltLines(): List<OptBeltLine> = beltLines.entries.map { (id, line) ->
    val tiles = List(line.tiles.size) { i ->
        val pos = line.getTilePos(i)
        val options = this[pos]!!.selectedBelt.mapNotNull { (entry, placement) ->
            val (direction) = entry
            if (direction != line.direction) return@mapNotNull null
            if (id !in placement.lineIds) return@mapNotNull null
            placement
        }
        val tile = line.tiles[i]
        val originalOption = tile.originalNode?.getBeltType()?.let { type ->
            options.find { it.beltType == type }
        }
        val mustNotBeEmpty = i == 0 || i == line.tiles.lastIndex || tile.mustBeNotEmpty || tile.mustMatch != null
        OptTile(
            origTile = tile,
            position = pos,
            options = options,
            originalOption = originalOption,
            mustNotBeEmpty = mustNotBeEmpty,
        )
    }
    OptBeltLine(
        id = id,
        origLine = line,
        tiles = tiles,
    )
}

private fun getTileInfo(grid: BeltPlacements, costs: InitialSolutionParams): Map<TilePosition, TileInfo> {
    val tileInfo = grid.tiles.mapValues {
        val canPlaceBelt = grid.model.placements.getInTile(it.key).none { it.isFixed } && costs.canUseTile(it.key)
        TileInfo(it.key, canPlaceBelt)
    }
    for ((id, line) in grid.beltLines) {
        for ((i, tile) in line.tiles.withIndex()) {
            val info = tileInfo[line.getTilePos(i)]!!
            info.numLines++
            if (tile.mustMatch != null || tile.mustBeNotEmpty) {
                info.onlyUsableBy = id
            }
        }
    }
    return tileInfo
}


private fun solveSingleBeltLine(
    tileInfo: Map<TilePosition, TileInfo>,
    line: OptBeltLine,
    costs: InitialSolutionParams,
): List<Pair<Int, BeltPlacement>> {
    val size = line.tiles.size

    // dp
    // cost of ending with belt or output underground
    val withOutputCost = DoubleArray(size) { Double.POSITIVE_INFINITY }
    // the prototype used for ^
    val outputUsed = arrayOfNulls<BeltPlacement>(size)
    // if ^ is output ug, index of connecting input ug
    // exception: isolated output ug at beginning
    val ugPairIndex = IntArray(size) { -1 }
    // cost of ending with input underground
    val ugInputCost = line.ugPrototypes.associateWith { DoubleArray(size) { Double.POSITIVE_INFINITY } }
    val ugInputUsed = line.ugPrototypes.associateWith { arrayOfNulls<BeltPlacement>(size) }
    // largest index of internal ug that can reach here
    var lastUgInPos = line.ugPrototypes.associateWithTo(mutableMapOf()) { 0 }

    fun costToUse(
        option: BeltPlacement,
        originalOption: BeltPlacement?,
        tile: TileInfo,
    ): Double {
        var cost = option.placement.cost
        if (tile.numLines > 1) {
            // discount intersection cost if matches original
            cost += if (option == originalOption) tile.intersectionCost * costs.intersectionDiscountMultiplier else tile.intersectionCost
        }
        return cost
    }

    for ((i, tile) in line.tiles.withIndex()) {
        val pos = tile.position

        // update lastUgInPos to be furthest reachable to here
        for (ug in line.ugPrototypes) {
            var ugI = lastUgInPos[ug]!!
            val inputCosts = ugInputCost[ug]!!
            while (ugI < i && (
                        (i - ugI) > ug.max_distance.toInt() || inputCosts[ugI].isInfinite())
            ) ugI++
            lastUgInPos[ug] = ugI
        }

        // skip updating values if tile is blocked
        val canUse = tileInfo[pos]!!.canHaveId(line.id)
        if (!canUse) {
            if (tile.mustNotBeEmpty) error("Tile must not be empty, but is blocked")
            continue
        }

        // update dp for options
        options@ for (option in tile.options) {
            val costToUseThis = costToUse(option, tile.originalOption, tileInfo[pos]!!)
            when (val beltType = option.beltType) {
                is BeltType.Belt -> {
                    val prevCost = if (i == 0) 0.0 else withOutputCost[i - 1]
                    val thisCost = prevCost + costToUseThis
                    if (thisCost < withOutputCost[i]) {
                        withOutputCost[i] = thisCost
                        outputUsed[i] = option
                    }
                }

                is BeltType.OutputUnderground -> {
                    // todo: bottom heuristic is wrong with non-uniform costs
                    val ugInPos = lastUgInPos[beltType.prototype]!!
                    val ugCost = when {
                        beltType.isIsolated -> 0.0 // edge case
                        ugInPos >= i -> continue@options // not possible
                        else -> ugInputCost[beltType.prototype]!![ugInPos]
                    }
                    val thisCost = ugCost + costToUseThis
                    if (thisCost < withOutputCost[i]) {
                        withOutputCost[i] = thisCost
                        outputUsed[i] = option
                        ugPairIndex[i] = ugInPos
                    }
                }

                is BeltType.InputUnderground -> {
                    val prevCost = if (i == 0) 0.0 else withOutputCost[i - 1]
                    val thisCost = prevCost + costToUseThis
                    val arr = ugInputCost[beltType.prototype]!!
                    if (thisCost < arr[i]) {
                        arr[i] = thisCost
                        ugInputUsed[beltType.prototype]!![i] = option
                    }
                }
            }
        }

        // forbid underground skipping if mustNotBeEmpty
        if (tile.mustNotBeEmpty) {
            lastUgInPos.iterator().forEach { entry -> entry.setValue(i) }
        }
    }
    // recover solution
    val lastIndex = size - 1
    var curIndex = lastIndex
    val placements = mutableListOf<Pair<Int, BeltPlacement>>()

    // edge case to handle isolated input ug at the end
    val finalIsolatedProto =
        (line.tiles.last().origTile.mustMatch as? BeltType.InputUnderground)
            ?.takeIf { it.isIsolated }
            ?.prototype
    if (finalIsolatedProto != null) {
        placements += lastIndex to ugInputUsed[finalIsolatedProto]!![lastIndex]!!
        curIndex--
    }

    val cost =
        if (finalIsolatedProto != null) ugInputCost[finalIsolatedProto]!![lastIndex] else withOutputCost[lastIndex]
    check(cost.isFinite()) { "Impossible to reach end" }

    // backtrace to get solution
    while (curIndex >= 0) {
        val placementUsed = outputUsed[curIndex]!!
        placements += curIndex to placementUsed
        val pairIndex = ugPairIndex[curIndex]
        if (pairIndex != -1 && curIndex != 0) {
            val inputPlacement = ugInputUsed[placementUsed.beltType.prototype]!![pairIndex]!!
            placements += pairIndex to inputPlacement
            curIndex = pairIndex - 1
        } else {
            curIndex--
        }
    }
    return placements.also {
//        it.reverse()
        line.curSolution = it
    }
}

private fun BeltPlacements.solveBeltLines(
    allLines: List<OptBeltLine>,
    costs: InitialSolutionParams,
    random: Random = Random.Default,
) {
    logger.info { "Finding initial solution" }
    val tileInfo = getTileInfo(this, costs)
    val linesById = allLines.associateBy { it.id }
    var curLines: List<OptBeltLine> = allLines.shuffled(random)

    for (i in 0..<costs.maxConflictIterations) {
        // find solutions, mark intersections
        val intersections = mutableSetOf<TileInfo>()
        for (line in curLines) {


            val solution = solveSingleBeltLine(tileInfo, line, costs)
            for ((i) in solution) {
                val pos = line.tiles[i].position
                val tileInfo = tileInfo[pos]!!
                tileInfo.usedLines += line.id
                if (tileInfo.usedLines.size > 1)
                    intersections += tileInfo
            }
        }
        if (intersections.isEmpty()) return
        if (i == costs.maxConflictIterations - 1) break

        // increase cost of intersections
        for (tile in intersections) {
            tile.intersectionCost =
                if (tile.intersectionCost == 0.0) costs.initialIntersectionCost else tile.intersectionCost * costs.intersectionCostMultiplier
        }
        // rip up intersecting lines, try again
        val intersectingIds = intersections.flatMapTo(mutableSetOf()) { it.usedLines }
        for (tile in intersections) tile.usedLines.clear()
        curLines = intersectingIds.map { linesById[it]!! }
    }

    error("Failed to find a solution after ${costs.maxConflictIterations} iterations")
}
