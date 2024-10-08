package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.ops.ItemTransportGraph.Node
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import glassbricks.factorio.blueprint.prototypes.Loader1x1Prototype
import glassbricks.factorio.blueprint.prototypes.Loader1x2Prototype
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}
// this class may be reused in the future for more complex things?

// node types:
// - inserter
// - all belts: belt, ug belt, splitters
// - crafting machines
// - containers
// todo: trains (rails)

enum class LogisticsEdgeType {
    Inserter,
    Belt,
}

class ItemTransportGraph(
    val nodes: Set<Node>,
    val entityToNode: Map<BlueprintEntity, Node>,
    val beltGrid: Map<TilePosition, Node>,
) {
    fun addEdge(from: Node, to: Node, type: LogisticsEdgeType) {
        val edge = Edge(from, to, type)
        from.outEdges += edge
        to.inEdges += edge
    }

    fun removeEdge(edge: Edge) {
        edge.from.outEdges.remove(edge)
        edge.to.inEdges.remove(edge)
    }

    class Node(
        val entity: BlueprintEntity,
        val outEdges: MutableList<Edge>,
        val inEdges: MutableList<Edge>,
    ) : Entity<EntityPrototype> by entity {
        fun edgeTo(other: Node): Edge? = outEdges.find { it.to == other }
        fun edgeFrom(other: Node): Edge? = inEdges.find { it.from == other }

        fun inEdges(type: LogisticsEdgeType): List<Edge> = inEdges.filter { it.type == type }
        fun outEdges(type: LogisticsEdgeType): List<Edge> = outEdges.filter { it.type == type }

        override fun toString(): String = "Node(entity=$entity)"
        var isWeavedUndergroundBelt = false
            internal set
    }

    class Edge(
        val from: Node,
        val to: Node,
        val type: LogisticsEdgeType,
    )
}

fun Node.hasSidewaysInput(): Boolean =
    (entity is TransportBelt || entity is UndergroundBelt) &&
            inEdges.any { it.type == LogisticsEdgeType.Belt && it.from.direction != entity.direction }

fun getItemTransportGraph(source: SpatialDataStructure<BlueprintEntity>): ItemTransportGraph {
    logger.info { "Building item transport graph" }
    val nodes = mutableSetOf<Node>()
    val entityToNode = mutableMapOf<BlueprintEntity, Node>()
    val belts = mutableMapOf<TilePosition, Node>()
    for (entity in source) {
        if (entity is Inserter
            || entity is TransportBeltConnectable
            || entity is CraftingMachine
            || entity is Container
            || entity is Lab
            || entity is MiningDrill
        ) {
            val node = Node(entity, mutableListOf(), mutableListOf())
            nodes += node
            entityToNode[entity] = node
        }
        if (entity is TransportBeltConnectable) {
            val node = entityToNode[entity]!!
            entity.collisionBox.roundOutToTileBbox().forEach { pos -> belts[pos] = node }
        }
    }
    val graph = ItemTransportGraph(nodes, entityToNode, belts)

    fun addInserterEdges(entity: Inserter, node: Node) {
        val pickupEntity = source.getIntersectingPosition(entity.globalPickupPosition())
            .firstNotNullOfOrNull { if (it.prototype is InserterPrototype) null else entityToNode[it] }
        if (pickupEntity != null) {
            graph.addEdge(pickupEntity, node, LogisticsEdgeType.Inserter)
        }
        val dropEntity = source.getIntersectingPosition(entity.globalInsertPosition())
            .firstNotNullOfOrNull { if (it.prototype is InserterPrototype) null else entityToNode[it] }
        if (dropEntity != null) {
            graph.addEdge(node, dropEntity, LogisticsEdgeType.Inserter)
        }
    }

    fun addBeltEdge(beltNode: Node, outputTile: TilePosition) {
        val outputNode = belts[outputTile]
        if (outputNode?.entity is TransportBeltConnectable &&
            outputNode.entity.canAcceptBeltInputFrom(outputTile, beltNode.entity.direction)
        ) {
            graph.addEdge(beltNode, outputNode, LogisticsEdgeType.Belt)
        }
    }


    fun addMinerEdge(miner: MiningDrill, node: Node) {
        val outputVector = miner.prototype.vector_to_place_result
        if (outputVector.isZero()) return
        val outputTile = (miner.position + outputVector.rotate(miner.direction))
            .occupiedTile()
        val outputNode = belts[outputTile]
        if (outputNode?.entity is TransportBeltConnectable) {
            graph.addEdge(node, outputNode, LogisticsEdgeType.Inserter)
        }
    }
    for (node in nodes) when (val entity = node.entity) {
        is Inserter -> addInserterEdges(entity, node)
        is MiningDrill -> addMinerEdge(entity, node)
        is TransportBeltConnectable -> {
            entity.getOutputTile1()?.let { addBeltEdge(node, it) }
            entity.getOutputTile2()?.let { addBeltEdge(node, it) }
            if (entity is UndergroundBelt) {
                val pair = entity.findForwardPair(source)
                val pairNode = pair?.let { entityToNode[it]!! }
                if (pair != null) {
                    graph.addEdge(node, pairNode!!, LogisticsEdgeType.Belt)
                    if (isBeltWeaving(entity, pair, source)) {
                        node.isWeavedUndergroundBelt = true
                        pairNode.isWeavedUndergroundBelt = true
                    }
                }
            }
        }
    }

    return graph
}


/**
 * Source entity is in _opposite_ direction of [beltDirection]
 */
fun TransportBeltConnectable.canAcceptBeltInputFrom(
    targetTile: TilePosition,
    beltDirection: Direction,
): Boolean {
    if (targetTile.tileCenter() !in collisionBox) return false
    return when (this) {
        is TransportBelt -> direction != beltDirection.oppositeDirection()
        is UndergroundBelt -> when (ioType) {
            IOType.Input -> direction != beltDirection.oppositeDirection() // same direction or sides
            IOType.Output -> (direction != beltDirection && direction != beltDirection.oppositeDirection()) // only sides
        }

        is LinkedBelt -> ioType == IOType.Input && direction == beltDirection
        is Splitter, is Loader -> direction == beltDirection
    }
}

fun TransportBeltConnectable.getOutputTile1(): TilePosition? {
    if (this is WithIoType && ioType != IOType.Output) return null
    return when (this) {
        is TransportBelt, is LinkedBelt, is UndergroundBelt -> position.occupiedTile() + direction.toTilePosVector()
        is Loader -> when (prototype) {
            is Loader1x1Prototype -> position.occupiedTile() + direction.toTilePosVector()
            is Loader1x2Prototype -> (position + direction.toPosVector() * 1.5).occupiedTile()
        }

        is Splitter -> (position + pos(-0.5, -1.0).rotateCardinal(direction)).occupiedTile()
    }
}

fun TransportBeltConnectable.getOutputTile2(): TilePosition? {
    if (this !is Splitter) return null
    return (position + pos(0.5, -1.0).rotateCardinal(direction)).occupiedTile()
}

fun UndergroundBelt.findForwardPair(entities: SpatialDataStructure<BlueprintEntity>): UndergroundBelt? {
    if (ioType != IOType.Input) return null
    val start = position.occupiedTile()
    for (i in 1..this.prototype.max_distance.toInt()) {
        val tile = start.shifted(direction, i)
        val ug = entities.getInTile(tile)
            .firstOrNull {
                it is UndergroundBelt &&
                        it.prototype == this.prototype &&
                        it.direction == this.direction
            }
            ?.let { it as UndergroundBelt }
        if (ug != null) {
            if (ug.ioType == IOType.Output) {
                return ug // matching pair
            } else {
                break // this blocks pairs
            }
        }
    }
    return null
}

fun isBeltWeaving(
    a: UndergroundBelt,
    b: UndergroundBelt,
    entities: SpatialDataStructure<BlueprintEntity>,
): Boolean {
    val dist = a.position.occupiedTile().manhattanDistanceTo(b.position.occupiedTile())
    val startUg = if (a.ioType == IOType.Input) a else b
    val start = startUg.position.occupiedTile()
    val direction = a.direction
    val prototype = a.prototype
    for (i in 1..<dist) {
        val tile = start.shifted(direction, i)
        val hasWeave = entities.getInTile(tile)
            .any {
                it is UndergroundBelt &&
                        it.prototype != prototype &&
                        (it.direction == direction || it.direction == direction.oppositeDirection())
            }
        if (hasWeave) return true
    }
    return false
}
