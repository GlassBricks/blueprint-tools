package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.ops.ItemTransportGraph
import glassbricks.factorio.blueprint.placement.ops.LogisticsEdgeType
import glassbricks.factorio.blueprint.placement.ops.getItemTransportGraph
import glassbricks.factorio.blueprint.placement.ops.hasSidewaysInput
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.placement.toCardinalDirection
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun ItemTransportGraph.Node.isIsolatedUnderground(): Boolean {
    if (entity !is UndergroundBelt) return false
    return when (entity.ioType) {
        IOType.Input -> outEdges.none { it.type == LogisticsEdgeType.Belt && it.to.entity is UndergroundBelt }
        IOType.Output -> inEdges.none { it.type == LogisticsEdgeType.Belt && it.from.entity is UndergroundBelt }
    }
}

fun ItemTransportGraph.Node.getBeltType(): BeltType? = when (val prototype = prototype) {
    is TransportBeltPrototype -> BeltType.Belt(prototype)
    is UndergroundBeltPrototype -> when ((entity as UndergroundBelt).ioType) {
        IOType.Input -> BeltType.InputUnderground(
            prototype,
            isIsolated = outEdges.none { it.type == LogisticsEdgeType.Belt && it.to.entity is UndergroundBelt && it.to.direction == direction })

        IOType.Output -> BeltType.OutputUnderground(
            prototype,
            isIsolated = inEdges.none { it.type == LogisticsEdgeType.Belt && it.from.entity is UndergroundBelt && it.from.direction == direction })
    }

    else -> null
}

fun ItemTransportGraph.Node.beltShouldBeNotEmpty(): Boolean =
    inEdges.any { it.type != LogisticsEdgeType.Belt } ||
            outEdges.any { it.type != LogisticsEdgeType.Belt }

fun ItemTransportGraph.Node.beltShouldMatchExisting(): Boolean =
    hasSidewaysInput() || entity is WithControlBehavior && entity.hasControlBehavior()
            || isIsolatedUnderground()

class BeltLine(
    val start: TilePosition,
    val direction: CardinalDirection,
    val tiles: List<BeltLineTile>,
    val outputsToNothing: Boolean = false,
)

data class BeltLineTile(
    val mustBeNotEmpty: Boolean,
    val allowedBeltTiers: Collection<BeltTier>,
    /** Has higher precedence than mustNotBeEmpty */
    val mustMatch: BeltType?,
)

fun GridConfig.addBeltLine(line: BeltLine) {
    val direction = line.direction
    val id = newLineId()
    for ((i, lineTile) in line.tiles.withIndex()) {
        val isStart = i == 0
        val isEnd = i == line.tiles.lastIndex
        val cell = this[line.start.shifted(direction, i)]
        if (isStart) {
            cell.makeLineStart(direction, id)
        }
        // a single tile line can be both start and end
        if (isEnd) {
            cell.makeLineEnd(direction, id)
        }
        if (lineTile.mustMatch != null) {
            cell.forceAs(direction, id, lineTile.mustMatch)
        } else {
            for (tier in lineTile.allowedBeltTiers) {
                cell.addOption(direction, tier.belt, id)
                if (!isEnd)
                    cell.addOption(direction, tier.inputUg, id)
                if (!isStart)
                    cell.addOption(direction, tier.outputUg, id)
            }
            if (lineTile.mustBeNotEmpty) {
                cell.forceIsId(id)
            }
        }
    }
    if (line.outputsToNothing) {
        val cell = this[line.start.shifted(direction, line.tiles.size)]
        cell.mustNotTakeInputIn(direction)
    }
}

fun getBeltLines(
    entities: SpatialDataStructure<BlueprintEntity>,
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): List<BeltLine> {
    return getBeltLinesFromTransportGraph(getItemTransportGraph(entities), prototypes)
}

fun findBeltLineFrom(
    initialNode: ItemTransportGraph.Node,
    beltTierMap: Map<out EntityPrototype, BeltTier>,
): Pair<BeltLine, Set<ItemTransportGraph.Node>>? {
    val direction = initialNode.direction.toCardinalDirection() ?: return null
    fun ItemTransportGraph.Node.isPartOfLine(): Boolean =
        this.direction == initialNode.direction && (this.entity is TransportBelt || this.entity is UndergroundBelt)

    if (!initialNode.isPartOfLine()) return null

    // traverse backwards while possible to find start node
    var curNode = initialNode
    while (true) {
        val prevNode = curNode.inEdges(LogisticsEdgeType.Belt)
            .find { it.from.isPartOfLine() }
            ?.from ?: break
        curNode = prevNode
    }
    val startNode = curNode

    class LineTileMut(
        val origNode: ItemTransportGraph.Node?,
    ) {
        var allowedBeltTiers: List<BeltTier>? = null
    }

    val startTile = startNode.position.occupiedTile()
    val tiles = mutableListOf<LineTileMut>()

    fun ItemTransportGraph.Node.index() = position.occupiedTile().manhattanDistanceTo(startTile)
    var segmentStartIndex = 0
    var curSegmentTiers = mutableListOf<BeltTier>()


    var cachedTierLists = mutableListOf<List<BeltTier>>()

    fun endSegment(endIndex: Int) {
        if (segmentStartIndex > endIndex) return
        curSegmentTiers.sort()
        val segmentTiers =
            cachedTierLists.find { it == curSegmentTiers } ?: curSegmentTiers.also {
                cachedTierLists += it
                curSegmentTiers = mutableListOf()
            }

        while (segmentStartIndex <= endIndex) {
            tiles[segmentStartIndex].allowedBeltTiers = segmentTiers
            segmentStartIndex++
        }
    }


    val nodes = mutableSetOf<ItemTransportGraph.Node>()
    fun visitNode(node: ItemTransportGraph.Node) {
        nodes += node
        val thisIndex = node.index()
        while (tiles.size < thisIndex) {
            tiles += LineTileMut(null)
        }
        tiles += LineTileMut(node)

        val nodeHasOtherInput = node.inEdges.any { !it.from.isPartOfLine() }
        if (nodeHasOtherInput) {
            endSegment(thisIndex - 1)
        }

        val nodeTier = beltTierMap[node.entity.prototype]!!
        if (nodeTier !in curSegmentTiers) curSegmentTiers += nodeTier
        assert(tiles.lastIndex == thisIndex)
        assert(segmentStartIndex <= thisIndex)

        val nodeHasOtherOutput = node.outEdges.any { !it.to.isPartOfLine() }
        if (nodeHasOtherOutput) {
            endSegment(thisIndex)
        }
    }

    visitNode(startNode)
    curNode = startNode
    while (true) {
        val nextNode = curNode.outEdges(LogisticsEdgeType.Belt)
            .firstOrNull()?.to
            ?.takeIf { it.isPartOfLine() }
            ?: break
        curNode = nextNode
        visitNode(curNode)
    }
    endSegment(tiles.lastIndex)

    val resultTiles = tiles.map { tile ->
        val origNode = tile.origNode
        val shouldNotBeEmpty = origNode?.beltShouldBeNotEmpty() == true
        val shouldMatchExactly = origNode?.beltShouldMatchExisting() == true
        BeltLineTile(
            mustBeNotEmpty = shouldNotBeEmpty,
            mustMatch = if (shouldMatchExactly) origNode.getBeltType() else null,
            allowedBeltTiers = tile.allowedBeltTiers!!
        )
    }

    val lastNode = curNode
    val outputsToNothing = lastNode.entity.let {
        it is Belt || it is UndergroundBelt && it.ioType == IOType.Output
    } && lastNode.outEdges.none { it.type == LogisticsEdgeType.Belt }

    return BeltLine(
        start = startTile,
        direction = direction,
        tiles = resultTiles,
        outputsToNothing = outputsToNothing,
    ) to nodes
}

fun getBeltLinesFromTransportGraph(
    transportGraph: ItemTransportGraph,
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): List<BeltLine> {
    logger.info { "Finding belt lines" }
    val beltTiers = prototypes.getAllBeltTiers()
    val visited = mutableSetOf<ItemTransportGraph.Node>()
    val result = transportGraph.nodes.mapNotNull {
        if (it in visited) return@mapNotNull null
        val lineInfo = findBeltLineFrom(it, beltTiers) ?: return@mapNotNull null
//        check(lineInfo.second.none { it in visited })
        visited += lineInfo.second
        lineInfo.first
    }
    return result
}

fun EntityPlacementModel.addBeltLinesFrom(
    origEntities: SpatialDataStructure<BlueprintEntity>,
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): Grid {
    logger.info { "Start: add belt lines" }
    return addBeltLinesFrom(getItemTransportGraph(origEntities), prototypes)
}

// this function can probably be dissected for more advanced usage
fun EntityPlacementModel.addBeltLinesFrom(
    transportGraph: ItemTransportGraph,
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): Grid {
    val grid = GridConfig()
    val beltLines = getBeltLinesFromTransportGraph(transportGraph, prototypes)
    for (line in beltLines) {
        grid.addBeltLine(line)
    }
    return this.addBeltGrid(grid)
}
