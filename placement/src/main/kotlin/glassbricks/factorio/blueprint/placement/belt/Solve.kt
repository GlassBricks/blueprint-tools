package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.SlidingWindowMin
import glassbricks.factorio.blueprint.placement.to8wayDirection
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

data class BeltLineSolveParams(
    val initialIntersectionCost: Double = 0.2,
    val intersectionCostMultiplier: Double = 2.0,
    /** Discount for intersection cost if the original belt is used. */
    val intersectionDiscountMultiplier: Double = 0.1,
    val maxConflictIterations: Int = 100,
    val random: Random = Random.Default,
) {
    init {
        require(initialIntersectionCost > 0)
        require(intersectionCostMultiplier > 1)
        require(intersectionDiscountMultiplier in 0.0..1.0)
        require(maxConflictIterations > 0)
    }
}

fun interface BeltLineCosts {
    fun getCost(beltType: BeltType, pos: TilePosition, direction: CardinalDirection): Double
}


fun BeltLineCosts.notIntersecting(entities: SpatialDataStructure<Entity<*>>): BeltLineCosts =
    BeltLineCosts { beltType, pos, direction ->
        val testEntity = beltType.prototype.placedAtTile(pos, direction.to8wayDirection())
        if (entities.getColliding(testEntity).any()) return@BeltLineCosts Double.POSITIVE_INFINITY
        getCost(beltType, pos, direction)
    }

fun fromMapAndDistance(
    costMap: Map<out EntityPrototype, Double>,
    center: Position,
    distanceCostFactor: Double,
): BeltLineCosts = BeltLineCosts { beltType, pos, _ ->
    val prototype = beltType.prototype
    val cost = costMap[prototype] ?: 1.0
    val distance = center.distanceTo(pos.tileCenter())
    cost + distance * distanceCostFactor
}

data class BeltTypeWithIndex(val beltType: BeltType, val index: Int)
data class BeltTypeWithCost(val beltType: BeltType, val cost: Double)

class OptBeltLine internal constructor(
    val id: BeltLineId,
    val origLine: BeltLine,
    internal val tiles: List<OptTile>,
) {
    internal var curSolution: List<BeltTypeWithIndex>? = null
    val solution get() = curSolution ?: error("Solution not (yet) found")

    fun getTilePos(index: Int): TilePosition = tiles[index].position

    fun solutionEntities(entitiesToCopyFrom: SpatialDataStructure<BlueprintEntity>? = null): List<BlueprintEntity> =
        solution.map {
            it.beltType.toBlueprintEntity(
                position = getTilePos(it.index),
                direction = origLine.direction,
                entitiesToCopyFrom = entitiesToCopyFrom,
            )
        }
}

internal class OptTile(
    val origTile: BeltLineTile,
    val position: TilePosition,
    val options: List<BeltTypeWithCost>,
    val originalBeltType: BeltType?,
    val mustBeNotEmpty: Boolean,
)

fun solveBeltLines(
    grid: BeltGridCommon,
    costs: BeltLineCosts,
    params: BeltLineSolveParams,
): List<OptBeltLine> {
    val lines = getOptBeltLines(grid, costs)
    iterativeSolve(lines, params)
    return lines
}

private fun getOptBeltLines(grid: BeltGridCommon, costs: BeltLineCosts): List<OptBeltLine> =
    grid.beltLines.entries.map { (lineId, line) ->
        val tiles = line.tiles.mapIndexed { i, tile ->
            val pos = line.getTilePos(i)
            val beltConfig = grid.tiles[pos]
            val options = beltConfig?.options?.mapNotNull { (direction, beltType, id) ->
                if (direction != line.direction || id != lineId) return@mapNotNull null
                val cost = costs.getCost(beltType, pos, direction)
                if (cost.isInfinite()) return@mapNotNull null
                BeltTypeWithCost(beltType, cost)
            }
                .orEmpty()
            val originalBeltType = tile.originalNode?.getBeltType()?.takeIf { beltType ->
                options.any { it.beltType == beltType }
            }
            OptTile(
                origTile = tile,
                position = pos,
                options = options,
                originalBeltType = originalBeltType,
                mustBeNotEmpty = (beltConfig != null && beltConfig.forcedId == lineId),
            )
        }
        OptBeltLine(
            id = lineId,
            origLine = line,
            tiles = tiles,
        )
    }

private class TileInfo {
    var onlyUsableBy: BeltLineId = 0
        set(value) {
            if (field != 0 && field != value)
                error("onlyUsableBy set twice")
            field = value
        }
    var numLines = 0

    var intersectionCost: Double = 0.0

    fun canHaveId(id: BeltLineId): Boolean = onlyUsableBy == 0 || onlyUsableBy == id

    val usedLines = mutableSetOf<BeltLineId>()
}

private fun getTileInfo(lines: List<OptBeltLine>): Map<TilePosition, TileInfo> = buildMap {
    for (line in lines) {
        for ((i, tile) in line.tiles.withIndex()) {
            val pos = line.origLine.getTilePos(i)
            val info = this.getOrPut(pos, ::TileInfo)
            info.numLines++
            if (tile.mustBeNotEmpty)
                info.onlyUsableBy = line.id
        }
    }
}

private fun solveSingleBeltLine(
    tileInfo: Map<TilePosition, TileInfo>,
    line: OptBeltLine,
    costs: BeltLineSolveParams,
): List<BeltTypeWithIndex> {
    val size = line.tiles.size

    // dp
    class UgInput(val index: Int, val cost: Double, val beltType: BeltType.InputUnderground)

    val comparator = Comparator<UgInput> { a, b -> a.cost.compareTo(b.cost) }
    // cost of ending with belt or output underground at i
    val withOutputCost = DoubleArray(size) { Double.POSITIVE_INFINITY }
    // the type used for ^
    val outputUsed = arrayOfNulls<BeltType>(size)
    // if type is an output underground, the input underground used
    val ugPair = arrayOfNulls<UgInput>(size)
    val ugInputsMap = mutableMapOf<UndergroundBeltPrototype, SlidingWindowMin<UgInput>>()
    fun ugInputs(prototype: UndergroundBeltPrototype) = ugInputsMap.getOrPut(prototype) { SlidingWindowMin(comparator) }

    for ((i, tile) in line.tiles.withIndex()) {
        val pos = tile.position

        // update lastUgInPos to be furthest reachable to here
        for ((ug, min) in ugInputsMap) {
            val minInd = i - ug.max_distance.toInt()
            min.removeWhile { it.index < minInd }
        }

        // skip updating values if tile is blocked
        val canUse = tileInfo[pos]!!.canHaveId(line.id)
        if (!canUse) {
            if (tile.mustBeNotEmpty) error("Tile must not be empty, but is blocked")
            continue
        }

        val tileInfo = tileInfo[pos]!!
        // update dp for options
        options@ for (option in tile.options) {
            var costToUseThis = option.cost
            if (tileInfo.numLines > 1) {
                // discount intersection cost if matches original
                costToUseThis += if (option.beltType == tile.originalBeltType) {
                    tileInfo.intersectionCost * costs.intersectionDiscountMultiplier
                } else {
                    tileInfo.intersectionCost
                }
            }
            when (val beltType = option.beltType) {
                is BeltType.Belt -> {
                    val prevCost = if (i == 0) 0.0 else withOutputCost[i - 1]
                    val thisCost = prevCost + costToUseThis
                    if (thisCost < withOutputCost[i]) {
                        withOutputCost[i] = thisCost
                        outputUsed[i] = beltType
                        ugPair[i] = null
                    }
                }

                is BeltType.OutputUnderground -> {
                    val ugInputCost: Double
                    val ugInput: UgInput?
                    if (beltType.isIsolated) {
                        ugInputCost = 0.0
                        ugInput = null
                    } else {
                        ugInput = ugInputsMap[beltType.prototype]?.min() ?: continue
                        ugInputCost = ugInput.cost
                    }
                    val thisCost = ugInputCost + costToUseThis
                    if (thisCost < withOutputCost[i]) {
                        withOutputCost[i] = thisCost
                        outputUsed[i] = beltType
                        ugPair[i] = ugInput
                    }
                }

                is BeltType.InputUnderground -> {
                    val prevCost = if (i == 0) 0.0 else withOutputCost[i - 1]
                    val thisCost = prevCost + costToUseThis
                    ugInputs(beltType.prototype).add(UgInput(i, thisCost, beltType))
                }
            }
        }

        // forbid underground skipping if mustNotBeEmpty
        if (tile.mustBeNotEmpty) {
            for (min in ugInputsMap.values) min.removeWhile { it.index < i }
        }
    }
    // recover solution
    val lastIndex = size - 1
    var curIndex = lastIndex
    val placements = mutableListOf<BeltTypeWithIndex>()

    // edge case to handle isolated input ug at the end
    val finalIsolatedProto =
        (line.tiles.last().origTile.mustMatch as? BeltType.InputUnderground)
            ?.takeIf { it.isIsolated }
            ?.prototype
    val lastIn = if (finalIsolatedProto != null) ugInputsMap[finalIsolatedProto]!!.back()!! else null

    val cost =
        if (lastIn != null) lastIn.cost
        else withOutputCost[lastIndex]
    check(cost.isFinite()) {
        "Impossible to reach end"
    }

    if (finalIsolatedProto != null) {
        placements += BeltTypeWithIndex(lastIn!!.beltType, curIndex)
        curIndex--
    }

    // backtrace to get solution
    while (curIndex >= 0) {
        val placementUsed = outputUsed[curIndex]!!
        placements += BeltTypeWithIndex(placementUsed, curIndex)
        val pair = ugPair[curIndex]
        if (pair != null) {
            val pairIndex = pair.index
            placements += BeltTypeWithIndex(pair.beltType, pairIndex)
            curIndex = pairIndex - 1
        } else {
            curIndex--
        }
    }
    return placements.also {
        line.curSolution = it
    }
}

private fun iterativeSolve(allLines: List<OptBeltLine>, params: BeltLineSolveParams) {
    logger.info { "Finding initial solution" }
    val tileInfo = getTileInfo(allLines)
    val linesById = allLines.associateBy { it.id }
    var curLines: List<OptBeltLine> = allLines.shuffled(params.random)

    for (i in 0..<params.maxConflictIterations) {
        // find solutions, mark intersections
        val intersections = mutableSetOf<TileInfo>()
        curLines.forEach { line -> solveSingleBeltLine(tileInfo, line, params) }
        for (line in curLines) {
            for ((_, i) in line.solution) {
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
        if (i == params.maxConflictIterations - 1) break

        // increase cost of intersections
        for (tile in intersections) {
            tile.intersectionCost =
                if (tile.intersectionCost == 0.0) params.initialIntersectionCost
                else tile.intersectionCost * params.intersectionCostMultiplier
        }
        // rip up intersecting lines, try again
        val intersectingIds = intersections.flatMapTo(mutableSetOf()) { it.usedLines }
        for (tile in intersections) tile.usedLines.clear()
        curLines = intersectingIds.map { linesById[it]!! }
        logger.debug { "Iteration ${i + 1}: ${intersections.size} intersections, ${curLines.size} lines" }
    }

    error("Failed to find a solution after ${params.maxConflictIterations} iterations")
}


// doesn't verify everything atm
fun verifySolution(
    resultEntities: SpatialDataStructure<BlueprintEntity>,
    lines: List<OptBeltLine>,
) {
    val origSig = getLinesSignature(lines.map { it.origLine })
    val curSig = getLinesSignature(getBeltLines(resultEntities))

    assertEqualsFancy(origSig, curSig)
}

private fun <K, V> assertEqualsFancy(expected: Map<K, V>, actual: Map<K, V>) {
    val missing = expected.keys - actual.keys
    val extra = actual.keys - expected.keys
    if (missing.isNotEmpty() || extra.isNotEmpty()) {
        error("Missing keys: $missing, extra keys: $extra")
    }
    for ((k, v) in expected) {
        if (v != actual[k]) {
            error("At key $k, expected $v, got ${actual[k]}")
        }
    }
}

private fun getLinesSignature(lines: List<BeltLine>): Map<Pair<TilePosition, CardinalDirection>, Int> =
    lines.associate {
        (it.start to it.direction) to it.tiles.size
    }
