package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.belt.BeltGridConfig
import glassbricks.factorio.blueprint.placement.belt.BeltGridVars
import glassbricks.factorio.blueprint.placement.belt.BeltTier
import glassbricks.factorio.blueprint.placement.belt.BeltType
import glassbricks.factorio.blueprint.placement.belt.addBeltLine
import glassbricks.factorio.blueprint.placement.belt.addBeltPlacementsFromVars
import glassbricks.factorio.blueprint.placement.belt.getAllBeltTiers
import glassbricks.factorio.blueprint.placement.belt.getBeltType
import glassbricks.factorio.blueprint.placement.toCardinalDirection
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes

private fun ItemTransportGraph.Node.shouldBeNotEmpty(): Boolean =
    inEdges.any { it.type != LogisticsEdgeType.Belt } ||
            outEdges.any { it.type != LogisticsEdgeType.Belt }

private fun ItemTransportGraph.Node.shouldMatchExisting(): Boolean =
    hasSidewaysInput() || entity is WithControlBehavior && entity.hasControlBehavior()

class BeltLine(
    val start: TilePosition,
    val direction: CardinalDirection,
    val length: Int,
    val mustBeNotEmpty: Collection<TilePosition>,
    val mustMatchExisting: Map<TilePosition, BeltType>,
    val beltTiers: Set<BeltTier>,
)

internal fun BeltGridConfig.addBeltLine(line: BeltLine) {
    val id = this.addBeltLine(
        start = line.start,
        direction = line.direction,
        length = line.length,
        beltTiers = line.beltTiers,
    )
    for ((pos, type) in line.mustMatchExisting) {
        this[pos].forceAs(line.direction, id, type)
    }
    for (pos in line.mustBeNotEmpty) {
        this[pos].forceIsId(id)
    }
}

fun getBeltLines(
    entities: MutableSpatialDataStructure<BlueprintEntity>,
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): List<BeltLine> {
    return getBeltLinesFromTransportGraph(getItemTransportGraph(entities), prototypes)
}

private fun getBeltLinesFromTransportGraph(
    transportGraph: ItemTransportGraph,
    prototypes: BlueprintPrototypes,
): List<BeltLine> {
    val beltTiers = prototypes.getAllBeltTiers()
    val visited = mutableSetOf<ItemTransportGraph.Node>()

    fun findBeltLine(initialNode: ItemTransportGraph.Node): BeltLine? {
        if (initialNode in visited) return null
        val direction = initialNode.direction.toCardinalDirection() ?: return null
        fun ItemTransportGraph.Node.isPartOfLine(): Boolean =
            this.direction.toCardinalDirection() == direction
                    && (this.entity is TransportBelt || this.entity is UndergroundBelt)
        if (!initialNode.isPartOfLine()) return null
        val origNodes = mutableListOf<ItemTransportGraph.Node>()
        fun visitNode(node: ItemTransportGraph.Node) {
            visited += node
            origNodes += node
        }
        visitNode(initialNode)

        // expand backwards
        var startNode = initialNode
        while (true) {
            val backNode = startNode.inEdges(LogisticsEdgeType.Belt)
                .find { it.from.isPartOfLine() }
                ?.from
                ?: break
            visitNode(backNode)
            startNode = backNode
        }
        // expand forwards
        var endNode = initialNode
        while (true) {
            val forwardsNode = endNode.outEdges(LogisticsEdgeType.Belt)
                .firstOrNull()?.to
                ?.takeIf { it.isPartOfLine() }
                ?: break
            visitNode(forwardsNode)
            endNode = forwardsNode
        }

        val startPos = startNode.position.occupiedTile()
        val endPos = endNode.position.occupiedTile()
        val length = startPos.manhattanDistanceTo(endPos) + 1


        val mustBeNotEmpty = origNodes.filter { it.shouldBeNotEmpty() }.map { it.position.occupiedTile() }
        val mustMatchExisting = origNodes.filter { it.shouldMatchExisting() }
            .associate {
                it.position.occupiedTile() to it.entity.getBeltType()!!
            }
        val beltTiers = origNodes.mapTo(mutableSetOf()) { beltTiers[it.entity.prototype]!! }

        return BeltLine(
            start = startPos,
            direction = direction,
            length = length,
            mustBeNotEmpty = mustBeNotEmpty,
            mustMatchExisting = mustMatchExisting,
            beltTiers = beltTiers
        )
    }

    return transportGraph.nodes.mapNotNull { findBeltLine(it) }
}

// this function can probably be dissected for more advanced usage
fun EntityPlacementModel.addBeltLinesFrom(
    origEntities: MutableSpatialDataStructure<BlueprintEntity>,
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): BeltGridVars {
    val grid = BeltGridConfig()
    for (line in getBeltLines(origEntities, prototypes)) {
        grid.addBeltLine(line)
    }
    val gridVars = grid.compile(cp)
    this.addBeltPlacementsFromVars(gridVars)
    return gridVars
}
