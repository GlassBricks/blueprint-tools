package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.ops.ItemTransportGraph.Node
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import glassbricks.factorio.blueprint.prototypes.Loader1x1Prototype
import glassbricks.factorio.blueprint.prototypes.Loader1x2Prototype

// this class may be reused in the future for more complex things?

// node types:
// - inserter
// - all belts: belt, ug belt, splitters
// - crafting machines
// - containers
// todo: trains (rails), miners

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
    val nodes = mutableSetOf<Node>()
    val entityToNode = mutableMapOf<BlueprintEntity, Node>()
    val belts = mutableMapOf<TilePosition, Node>()
    for (entity in source) {
        if (entity is Inserter
            || entity is TransportBeltConnectable
            || entity is CraftingMachine
            || entity is Container
            || entity is Lab
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
    for (node in nodes) if (node.entity is Inserter) {
        val entity = node.entity
        val pickupEntity = source.getAtPoint(entity.globalPickupPosition())
            .firstNotNullOfOrNull { if (it.prototype is InserterPrototype) null else entityToNode[it] }
        if (pickupEntity != null) {
            graph.addEdge(pickupEntity, node, LogisticsEdgeType.Inserter)
        }
        val dropEntity = source.getAtPoint(entity.globalInsertPosition())
            .firstNotNullOfOrNull { if (it.prototype is InserterPrototype) null else entityToNode[it] }
        if (dropEntity != null) {
            graph.addEdge(node, dropEntity, LogisticsEdgeType.Inserter)
        }
    }

    fun addBeltEdge(beltNode: Node, outputTile: TilePosition) {
        val outputNode = belts[outputTile]
        if (outputNode?.entity is TransportBeltConnectable &&
            outputNode.entity.canAcceptInputFrom(outputTile, beltNode.entity.direction)
        ) {
            graph.addEdge(beltNode, outputNode, LogisticsEdgeType.Belt)
        }
    }

    for (beltNode in nodes) {
        val belt = beltNode.entity as? TransportBeltConnectable ?: continue
        belt.getOutputTile1()?.let { addBeltEdge(beltNode, it) }
        belt.getOutputTile2()?.let { addBeltEdge(beltNode, it) }
        (belt as? UndergroundBelt)?.findForwardPair(source)?.let { pairUg ->
            graph.addEdge(beltNode, entityToNode[pairUg]!!, LogisticsEdgeType.Belt)
        }
    }

    return graph
}


/**
 * Source entity is in _opposite_ direction of [beltDirection]
 */
fun TransportBeltConnectable.canAcceptInputFrom(
    targetTile: TilePosition,
    beltDirection: Direction,
): Boolean {
    if (targetTile.center() !in collisionBox) return false
    if (this is WithIoType && ioType != IOType.Input) return false
    return when (this) {
        is TransportBelt, is UndergroundBelt -> direction != beltDirection.oppositeDirection()
        is LinkedBelt, is Splitter -> direction == beltDirection
        is Loader -> direction == beltDirection
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
