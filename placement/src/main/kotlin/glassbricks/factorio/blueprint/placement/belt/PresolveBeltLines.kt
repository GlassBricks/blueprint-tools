package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.basicPlacedAtTile
import glassbricks.factorio.blueprint.placement.SlidingWindowMin
import glassbricks.factorio.blueprint.placement.addEquality
import glassbricks.factorio.blueprint.placement.addHint
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

data class InitialSolutionParams(
    val initialIntersectionCost: Double = 2.0,
    val intersectionCostMultiplier: Double = 1.5,
    /** Discount for intersection cost if the original belt is used. */
    val intersectionDiscountMultiplier: Double = 0.1,
    val maxConflictIterations: Int = 100,
    val canPlace: (Entity<*>) -> Boolean = { testEntity -> true },
    val sampleBelt: TransportBeltPrototype = VanillaPrototypes.getAs("transport-belt"),
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


private class TileInfo(var canPlaceBelt: Boolean) {
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
    val forcedOrMustBeNotEmpty: Boolean,
)

data class UsedBeltPlacement(
    val index: Int,
    val placement: BeltPlacement,
)
typealias BeltPlacementSolution = List<UsedBeltPlacement>

class OptBeltLine internal constructor(
    val id: BeltLineId,
    @Suppress("unused") val origLine: BeltLine,
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
            forcedOrMustBeNotEmpty = mustNotBeEmpty,
        )
    }
    OptBeltLine(
        id = id,
        origLine = line,
        tiles = tiles,
    )
}

private fun getTileInfo(grid: BeltPlacements, params: InitialSolutionParams): Map<TilePosition, TileInfo> {
    val tileInfo = grid.tiles.mapValues { (pos, _) ->
        val testEntity = params.sampleBelt.basicPlacedAtTile(pos)
        val canPlaceBelt = grid.model.canPlace(testEntity) && params.canPlace(testEntity)
        TileInfo(canPlaceBelt)
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
): List<UsedBeltPlacement> {
    val size = line.tiles.size

    // dp
    class UgInput(val index: Int, val cost: Double, val placement: BeltPlacement)
    // cost of ending with belt or output underground
    val withOutputCost = DoubleArray(size) { Double.POSITIVE_INFINITY }
    // the placement used for ^
    val outputUsed = arrayOfNulls<BeltPlacement>(size)
    // if placement is an output underground, the input underground used
    val ugPair = arrayOfNulls<UgInput>(size)

    val ugInputs = line.ugPrototypes.associateWith { SlidingWindowMin<UgInput>(compareBy { it.cost }) }

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
        for ((ug, min) in ugInputs) {
            val minInd = i - ug.max_distance.toInt()
            min.removeWhile { it.index < minInd }
        }

        // skip updating values if tile is blocked
        val canUse = tileInfo[pos]!!.canHaveId(line.id)
        if (!canUse) {
            if (tile.forcedOrMustBeNotEmpty) error("Tile must not be empty, but is blocked")
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
                        ugPair[i] = null
                    }
                }

                is BeltType.OutputUnderground -> {
                    val ugInputCost: Double
                    val ugInput: UgInput?
                    when {
                        beltType.isIsolated -> {
                            ugInputCost = 0.0
                            ugInput = null
                        }

                        else -> {
                            ugInput = ugInputs[beltType.prototype]!!.min() ?: continue
                            ugInputCost = ugInput.cost
                        }
                    }
                    val thisCost = ugInputCost + costToUseThis
                    if (thisCost < withOutputCost[i]) {
                        withOutputCost[i] = thisCost
                        outputUsed[i] = option
                        ugPair[i] = ugInput
                    }
                }

                is BeltType.InputUnderground -> {
                    val prevCost = if (i == 0) 0.0 else withOutputCost[i - 1]
                    val thisCost = prevCost + costToUseThis
                    val ugInput = UgInput(i, thisCost, option)
                    ugInputs[beltType.prototype]!!.add(ugInput)
                }
            }
        }

        // forbid underground skipping if mustNotBeEmpty
        if (tile.forcedOrMustBeNotEmpty) {
            for (min in ugInputs.values) min.removeWhile { it.index < i }
        }
    }
    // recover solution
    val lastIndex = size - 1
    var curIndex = lastIndex
    val placements = mutableListOf<UsedBeltPlacement>()

    // edge case to handle isolated input ug at the end
    val finalIsolatedProto =
        (line.tiles.last().origTile.mustMatch as? BeltType.InputUnderground)
            ?.takeIf { it.isIsolated }
            ?.prototype
    val lastIn = if (finalIsolatedProto != null) ugInputs[finalIsolatedProto]!!.back()!! else null

    val cost =
        if (lastIn != null) lastIn.cost
        else withOutputCost[lastIndex]
    check(cost.isFinite()) { "Impossible to reach end" }

    if (finalIsolatedProto != null) {
        placements += UsedBeltPlacement(curIndex, lastIn!!.placement)
        curIndex--
    }

    // backtrace to get solution
    while (curIndex >= 0) {
        val placementUsed = outputUsed[curIndex]!!
        placements += UsedBeltPlacement(curIndex, placementUsed)
        val pair = ugPair[curIndex]
        if (pair != null) {
            val pairIndex = pair.index
            placements += UsedBeltPlacement(pairIndex, pair.placement)
            curIndex = pairIndex - 1
        } else {
            curIndex--
        }
    }
    return placements.also {
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
        if (intersections.isEmpty()) {
            logger.info { "Found a solution on iteration ${i + 1}" }
            return
        }
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
        logger.debug { "Iteration ${i + 1}: ${intersections.size} intersections, ${curLines.size} lines" }
    }

    error("Failed to find a solution after ${costs.maxConflictIterations} iterations")
}
