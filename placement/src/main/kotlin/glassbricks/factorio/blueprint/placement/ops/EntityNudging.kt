package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.BasicEntity
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.CircuitConnectionPoint
import glassbricks.factorio.blueprint.entity.Inserter
import glassbricks.factorio.blueprint.entity.Rail
import glassbricks.factorio.blueprint.entity.TransportBeltConnectable
import glassbricks.factorio.blueprint.findMatching
import glassbricks.factorio.blueprint.globalInsertPosition
import glassbricks.factorio.blueprint.globalPickupPosition
import glassbricks.factorio.blueprint.placement.EntityPlacement
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.tileBbox

// NOTE: fluids not handled
// Nudging filter:
//  - Belts and user-supplied fixed entities are _not_ nudgeable
//  - Inserters are nudgeable if:
//     - not connected to a belt
//     - targets are nudgeable or have size > 1
//  Making nudges:
//  - For _non inserters_, generate all positions
//      - If only one possible position, add as fixed
//  - For inserters:
//      - Find possible positions, based on targets

fun BlueprintEntity.nudgingAllowed(): Boolean = !(
        this is TransportBeltConnectable
                || this is Rail
                || (this is CircuitConnectionPoint && !this.circuitConnections.isEmpty())
        )

/**
 * Assumes all other entities are added as fixed.
 *
 * Not yet compatible with belt optimization.
 */
fun EntityPlacementModel.addEntityNudging(
    entities: Collection<BlueprintEntity>,
    transportGraph: ItemTransportGraph,
    nudgeRange: Int = 2,
) {
    val nudgedPlacements: MutableMap<BlueprintEntity, List<EntityPlacement<*>>> = mutableMapOf()
    val nudgeDistances = tileBbox(-nudgeRange, -nudgeRange, nudgeRange + 1, nudgeRange + 1).map { it.tileTopLeft() }
    fun getNudgePositions(entity: BlueprintEntity): List<Position> {
        if (!entity.nudgingAllowed()) return listOf(entity.position)
        val positions = nudgeDistances.map { nudgeVec ->
            entity.position + nudgeVec
        }.filter {
            val testEntity = BasicEntity(entity.prototype, it, entity.direction)
            this.canPlace(testEntity)
        }
        check(positions.isNotEmpty()) { "No valid positions for $entity" }
        return positions
    }

    fun addAsFixed(entity: BlueprintEntity, position: Position) {
        val placement = this.addFixedPlacement(entity, position = position)
        nudgedPlacements[entity] = listOf(placement)
    }

    for (entity in entities) if (entity !is Inserter) {
        val positions = getNudgePositions(entity)
        if (positions.size == 1) {
            addAsFixed(entity, positions.first())
            continue
        }
        val placements = positions.map { this.addPlacement(entity, position = it) }
        this.cp.addExactlyOne(placements.map { it.selected })
        nudgedPlacements[entity] = placements
    }
    data class InserterLinks(
        val position: Position,
        val pickupPlacements: List<EntityPlacement<*>>?,
        val dropoffPlacements: List<EntityPlacement<*>>?,
    )
    for (entity in entities) if (entity is Inserter) {
        val node = transportGraph.entityToNode[entity]!!
        val pickupEntity = node.inEdges.find { it.type == LogisticsEdgeType.Inserter }?.from?.entity
        val dropoffEntity = node.outEdges.find { it.type == LogisticsEdgeType.Inserter }?.to?.entity

        val pickupPlacements = pickupEntity?.let {
            nudgedPlacements[it] ?: this.placements.findMatching(it)
                ?.let { listOf(it) }
            ?: error("Could not find placement for $it")
        }
        val dropoffPlacements = dropoffEntity?.let {
            nudgedPlacements[it] ?: this.placements.findMatching(it)
                ?.let { listOf(it) }
            ?: error("Could not find placement for $it")
        }
        val positions = if (pickupPlacements == null || dropoffPlacements == null) listOf(entity.position)
        else getNudgePositions(entity)
        val inserterLinks = positions.mapNotNull { position ->
            val testEntity = BasicEntity(entity.prototype, position, entity.direction)
            val pickupPos = testEntity.globalPickupPosition()
            val dropOffPos = testEntity.globalInsertPosition()
            val pickupPlacementsAtPos = pickupPlacements?.filter { it.collidesWithPoint(pickupPos) }
            if (pickupPlacementsAtPos?.isEmpty() == true) return@mapNotNull null
            val dropoffPlacementsAtPos = dropoffPlacements?.filter { it.collidesWithPoint(dropOffPos) }
            if (dropoffPlacementsAtPos?.isEmpty() == true) return@mapNotNull null
            InserterLinks(position, pickupPlacementsAtPos, dropoffPlacementsAtPos)
        }
        check(inserterLinks.isNotEmpty()) { "No valid positions for $entity" }
        val isSingle = inserterLinks.size == 1
        val placements = inserterLinks.map { (position, pickupPlacements, dropoffPlacements) ->
            val placement = if (isSingle) {
                this.addFixedPlacement(entity, position = position)
            } else {
                this.addPlacement(entity, position = position)
            }
            if (pickupPlacements != null) {
                this.cp.addBoolOr(pickupPlacements.map { it.selected }).onlyEnforceIf(placement.selected)
            }
            if (dropoffPlacements != null) {
                this.cp.addBoolOr(dropoffPlacements.map { it.selected }).onlyEnforceIf(placement.selected)
            }
            placement
        }
        if (!isSingle) {
            this.cp.addExactlyOne(placements.map { it.selected })
        }
    }
}
