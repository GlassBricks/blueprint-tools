package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltConnectablePrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.shifted
import kotlin.random.Random

class BeltCosts(
    entityCosts: Map<EntityPrototype, Double>,
    var overlapCost: Double = 0.5,
    var additionalCostFn: (
        prototype: TransportBeltConnectablePrototype,
        position: TilePosition,
        direction: Direction,
    ) -> Double = { _, _, _ -> 0.0 },
) {
    @Suppress("UNCHECKED_CAST")
    val beltCosts: Map<TransportBeltPrototype, Double> =
        entityCosts.filterKeys { it is TransportBeltPrototype } as Map<TransportBeltPrototype, Double>

    @Suppress("UNCHECKED_CAST")
    val ugBeltCosts: Map<UndergroundBeltPrototype, Double> =
        entityCosts.filterKeys { it is UndergroundBeltPrototype } as Map<UndergroundBeltPrototype, Double>

    val beltToUgBelt: Map<TransportBeltPrototype, UndergroundBeltPrototype>
    val ugBeltToBelt: Map<UndergroundBeltPrototype, TransportBeltPrototype>

    fun getAssociatedBelt(prototype: EntityPrototype): TransportBeltPrototype? =
        when (prototype) {
            is TransportBeltPrototype -> prototype.takeIf { it in beltCosts }
            is UndergroundBeltPrototype -> ugBeltToBelt[prototype]
            else -> null
        }


    init {
        for ((proto, cost) in entityCosts) {
            require(cost >= 0.0) { "Cost for $proto is negative: $cost" }
            require(proto is TransportBeltPrototype || proto is UndergroundBeltPrototype) { "Invalid prototype: $proto" }
        }
        val beltToUgBelt = mutableMapOf<TransportBeltPrototype, UndergroundBeltPrototype>()
        val ugBeltToBelt = mutableMapOf<UndergroundBeltPrototype, TransportBeltPrototype>()
        for (beltProto in beltCosts.keys) {
            val relatedUgBelt = ugBeltCosts.keys.find { it.name == beltProto.related_underground_belt }
            requireNotNull(relatedUgBelt) { "Cost given for belt $beltProto, but not for ${beltProto.related_underground_belt}" }
            beltToUgBelt[beltProto] = relatedUgBelt
            ugBeltToBelt[relatedUgBelt] = beltProto
        }
        for (ugBeltProto in ugBeltCosts.keys) {
            require(ugBeltProto in ugBeltToBelt) { "Cost given for ug belt $ugBeltProto, but not for its related belt" }
        }
        this.beltToUgBelt = beltToUgBelt
        this.ugBeltToBelt = ugBeltToBelt
    }
}

class BeltLine(
    val start: TilePosition,
    val direction: Direction,
    val isUgInput: Boolean,
    val isUgOutput: Boolean,
    val length: Int,
    val mustNotBeEmpty: Set<Int>,
    val originalEntities: Set<BlueprintEntity>,
    val beltPrototype: TransportBeltPrototype,
    val ugPrototype: UndergroundBeltPrototype,
    val costs: BeltCosts,
) {
    fun getTilePosition(index: Int): TilePosition = start.shifted(direction, index)
}

fun getBeltLines(
    entities: MutableSpatialDataStructure<BlueprintEntity>,
    costs: BeltCosts,
): List<BeltLine> {

    val graph = getItemTransportGraph(entities)
    for (entity in entities) {
        if (entity is TransportBelt) {
            graph.entityToNode[entity]!!
        }
    }
    val fixedNodes = mutableSetOf<ItemTransportGraph.Node>()

    for (node in graph.nodes) {
        val entity = node.entity
        if (!(entity is UndergroundBelt && entity.ioType == IOType.Input)) continue

        val pair = entity.findForwardPair(entities) ?: continue
        val pairTile = pair.position.occupiedTile()

        val start = entity.position.occupiedTile()
        val direction = entity.direction

        for (i in 1..entity.prototype.max_distance.toInt()) {
            val toCheck = start.shifted(direction, i)
            if (toCheck == pairTile) break
            val belt =
                entities.getInTile(toCheck)
                    .firstOrNull {
                        it is TransportBelt && (it.direction == direction
                                || it.direction == direction.oppositeDirection())
                    }
            if (belt != null) {
                fixedNodes += graph.entityToNode[belt]!!
            }
        }
    }
    val visited = mutableSetOf<ItemTransportGraph.Node>()

    fun ItemTransportGraph.Node.isFixed(): Boolean =
        this in fixedNodes ||
                this.hasInputFromSide() ||
                entity is TransportBelt && !entity.circuitConnections.isEmpty()

    fun getLineFrom(initialNode: ItemTransportGraph.Node): BeltLine? {
        val direction = initialNode.direction
        val beltPrototype = costs.getAssociatedBelt(initialNode.entity.prototype) ?: return null

        fun ItemTransportGraph.Node.isPartOfLine(): Boolean =
            this.entity.direction == direction
                    && costs.getAssociatedBelt(this.entity.prototype) == beltPrototype

        if (!initialNode.isPartOfLine() || initialNode.isFixed()) return null

        val mustNotBeEmpty = mutableListOf<ItemTransportGraph.Node>()
        val origEntities = mutableSetOf<BlueprintEntity>()
        fun visitNode(node: ItemTransportGraph.Node) {
            visited += node
            origEntities += node.entity
            if (node.inEdges.any { it.type != LogisticsEdgeType.Belt }
                || node.outEdges.any { it.type != LogisticsEdgeType.Belt }
            ) {
                mustNotBeEmpty += node
            }
        }
        visitNode(initialNode)

        // expand backwards
        var lastNode = initialNode
        val startPos: TilePosition
        val isUgInput: Boolean
        while (true) {
            val backwardsNode = lastNode.inEdges(LogisticsEdgeType.Belt)
                .find { it.from.direction == direction }?.from
                ?.takeIf { it.isPartOfLine() }
            if (backwardsNode == null) {
                startPos = lastNode.position.occupiedTile()
                isUgInput = false
                break
            }
            if (backwardsNode.isFixed()) {
                startPos = backwardsNode.position.occupiedTile() + direction.toTilePosVector()
                isUgInput = backwardsNode.entity is UndergroundBelt && backwardsNode.entity.ioType == IOType.Input
                break
            }
            lastNode = backwardsNode
            visitNode(lastNode)
        }
        // expand forwards
        lastNode = initialNode
        val endPos: TilePosition
        val isUgOutput: Boolean
        while (true) {
            val forwardsNode = lastNode.outEdges(LogisticsEdgeType.Belt).firstOrNull()
                ?.to
                ?.takeIf { it.isPartOfLine() }
            if (forwardsNode == null) {
                endPos = lastNode.position.occupiedTile()
                isUgOutput = false
                break
            }
            if (forwardsNode.isFixed()) {
                endPos = forwardsNode.position.occupiedTile() - direction.toTilePosVector()
                isUgOutput = forwardsNode.entity is UndergroundBelt && forwardsNode.entity.ioType == IOType.Output
                break
            }
            lastNode = forwardsNode
            visitNode(lastNode)
        }
        val length = startPos.manhattanDistanceTo(endPos) + 1
        if (length <= 2) return null

        return BeltLine(
            start = startPos,
            direction = direction,
            isUgInput = isUgInput,
            isUgOutput = isUgOutput,
            length = length,
            mustNotBeEmpty = mustNotBeEmpty.mapTo(hashSetOf()) {
                it.position.occupiedTile().manhattanDistanceTo(startPos)
            },
            originalEntities = origEntities,
            beltPrototype = beltPrototype,
            ugPrototype = costs.beltToUgBelt[beltPrototype]!!,
            costs = costs,
        )
    }

    val lines = mutableListOf<BeltLine>()

    for (node in graph.nodes) {
        if (node in visited || node in fixedNodes) continue; visited += node
        val line = getLineFrom(node) ?: continue
        lines += line
    }

    return lines
}

private fun ItemTransportGraph.Node.hasInputFromSide(): Boolean {
    if (entity !is TransportBelt) return false
    return inEdges.any { it.type == LogisticsEdgeType.Belt && it.from.entity.direction != this.direction }
}

fun optimizeBeltLinesInBp(
    entities: MutableSpatialDataStructure<BlueprintEntity>,
    costs: BeltCosts,
    numRetries: Int = 20,
    maxConflictsPerAttempt: Int = 20,
    random: Random = Random.Default,
) {
    val lines = getBeltLines(entities, costs)
    optimizeBeltLines(entities, lines, numRetries, random, maxConflictsPerAttempt)
}

fun getBeltLineCoverageMap(lines: List<BeltLine>): Map<TilePosition, Int> {
    val coverage = mutableMapOf<TilePosition, Int>()
    for (line in lines) {
        for (i in 0..<line.length) {
            val pos = line.start.shifted(line.direction, i)
            coverage[pos] = coverage.getOrDefault(pos, 0) + 1
        }
    }
    return coverage
}

/**
 * The very first try does not shuffle the lines.
 */
fun optimizeBeltLines(
    entities: MutableSpatialDataStructure<BlueprintEntity>,
    allLines: List<BeltLine>,
    numRetries: Int,
    random: Random = Random,
    maxConflictsPerAttempt: Int,
) {

    val coverage = getBeltLineCoverageMap(allLines)
    val beltLinesByPrio = mutableMapOf<Int, MutableList<BeltLine>>()
    beltLinesByPrio[0] = allLines.toMutableList()
    var maxPrio = 0

    retry@ for (i in 0..<numRetries) {
        for (list in beltLinesByPrio.values) list.shuffle(random)
        for (line in allLines) {
            entities.removeAll(line.originalEntities)
        }
        for ((prio, lines) in beltLinesByPrio) {
            if (prio != 0) for (line in lines) entities.addAll(line.originalEntities)
        }
        val addedEntities = mutableSetOf<BlueprintEntity>()
        var numConflicts = 0
        var prio = 0
        while (prio <= maxPrio) {
            val lines = beltLinesByPrio[prio]!!
            if (prio != 0) for (line in lines) entities.removeAll(line.originalEntities)
            val toRemove = mutableSetOf<BeltLine>()
            for (line in lines) {
                val thisLineEntities = optimizeBeltLineAndAdd(entities, line, coverage)
                if (thisLineEntities == null) {
                    numConflicts++
                    maxPrio = maxPrio.coerceAtLeast(prio + 1)
                    toRemove.add(line)
                    beltLinesByPrio.getOrPut(prio + 1) { mutableListOf() }.add(line)
                    if (numConflicts >= maxConflictsPerAttempt) break
                } else {
                    addedEntities.addAll(thisLineEntities)
                }
            }
            lines.removeAll(toRemove)
            prio++
            if (numConflicts >= maxConflictsPerAttempt) break
        }
        if (numConflicts > 0) {
            entities.removeAll(addedEntities)
            continue@retry
        } else {
            return
        }
    }
    error("Could not resolve all conflicts in $numRetries attempts")
}

fun optimizeBeltLineAndAdd(
    entities: MutableSpatialDataStructure<BlueprintEntity>,
    line: BeltLine,
    coverageMap: Map<TilePosition, Int>?,
): List<BlueprintEntity>? {
    // dp
    val beltOutputCost = DoubleArray(line.length + 1)
    val hopLength = IntArray(line.length + 1)
    val ugInputCost = DoubleArray(line.length + 1) { Double.POSITIVE_INFINITY }
    var lastInUgPos = 0
    if (line.isUgInput) {
        beltOutputCost[0] = Double.POSITIVE_INFINITY
        ugInputCost[0] = 0.0
    }

    val beltCost = line.costs.beltCosts[line.beltPrototype]!!
    val ugCost = line.costs.ugBeltCosts[line.ugPrototype]!!

    fun canPlaceBelt(position: TilePosition): Boolean = entities.getInTile(position).none()
    for (pos in 1..line.length) {
        if (!canPlaceBelt(line.start.shifted(line.direction, pos - 1))) {
            beltOutputCost[pos] = Double.POSITIVE_INFINITY
            hopLength[pos] = Int.MAX_VALUE
            ugInputCost[pos] = Double.POSITIVE_INFINITY
        } else {
            val tilePos = line.getTilePosition(pos - 1)
            val isOverlap = coverageMap != null && coverageMap[tilePos]?.let { it > 1 } ?: false
            val overlapCost = if (isOverlap) line.costs.overlapCost else 0.0

            val thisUgCost = ugCost + line.costs.additionalCostFn(
                line.ugPrototype,
                tilePos,
                line.direction
            ) + overlapCost
            val thisBeltCost = beltCost + line.costs.additionalCostFn(
                line.beltPrototype,
                tilePos,
                line.direction
            ) + overlapCost

            val usingBelt = beltOutputCost[pos - 1] + thisBeltCost
            val usingUG = ugInputCost[lastInUgPos] + thisUgCost
            if (usingBelt < usingUG) {
                beltOutputCost[pos] = usingBelt
                hopLength[pos] = 1
            } else {
                beltOutputCost[pos] = usingUG
                hopLength[pos] = pos - lastInUgPos + 1
            }
            ugInputCost[pos] = beltOutputCost[pos - 1] + thisUgCost
        }
        if ((pos - 1) in line.mustNotBeEmpty) {
            lastInUgPos = pos
        } else {
            while (
                ((pos + 1) - lastInUgPos > line.ugPrototype.max_distance.toInt()
                        || ugInputCost[lastInUgPos].isInfinite())
                && lastInUgPos < (pos)
            ) lastInUgPos++
        }
    }
    val resEntities = mutableListOf<BlueprintEntity>()
    // get answer
    var pos = line.length
    if (line.isUgOutput) {
        if (ugInputCost[lastInUgPos] == Double.POSITIVE_INFINITY) return null
        val inUgPos = line.start.shifted(line.direction, lastInUgPos - 1)
        val inUg = line.ugPrototype.placedAtTile(inUgPos, line.direction) as UndergroundBelt
        inUg.ioType = IOType.Input
        resEntities.add(inUg)
        pos = lastInUgPos - 1
    }

    if (beltOutputCost.last() == Double.POSITIVE_INFINITY)
        return null
    while (pos > 0) {
        val hopLen = hopLength[pos]
        check(hopLen != Int.MAX_VALUE)
        val position = line.start.shifted(line.direction, pos - 1)
        if (hopLen == 1) {
            val belt =
                line.beltPrototype.placedAtTile(position, line.direction)
            resEntities.add(belt)
        } else {
            val outUg = line.ugPrototype.placedAtTile(position, line.direction) as UndergroundBelt
            outUg.ioType = IOType.Output
            val inUgPos = line.start.shifted(line.direction, (pos - hopLen))
            val inUg = line.ugPrototype.placedAtTile(inUgPos, line.direction) as UndergroundBelt
            inUg.ioType = IOType.Input
            resEntities.add(outUg)
            resEntities.add(inUg)
        }
        pos -= hopLen
    }
    entities.addAll(resEntities)
    return resEntities
}
