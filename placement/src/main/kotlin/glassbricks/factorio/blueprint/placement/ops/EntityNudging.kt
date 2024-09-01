/*
package glassbricks.factorio.blueprint.placement.ops

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.globalInsertPosition
import glassbricks.factorio.blueprint.globalPickupPosition
import glassbricks.factorio.blueprint.placement.EntityPlacement
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.addBoolAndLenient
import glassbricks.factorio.blueprint.placement.addExactlyOneLenient
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import glassbricks.factorio.blueprint.tileBbox


private class InserterLinks(
    val pickupEntity: EntityPlacement<*>?,
    val insertEntity: EntityPlacement<*>?,
)

// NOTE: does not handle custom blueprint pickup and dropoff positions
fun EntityPlacementModel.addEntityNudgingWithInserters(
    entitiesToNudge: Set<EntityPlacement<*>>,
    nudgeRange: Int = 2,
    nudgeCost: Double = 0.01,
) {
    for (entity in entitiesToNudge) {
        require(entity in placements) { "Entity $entity is not in the model" }
        require(entity.isFixed) { "Non-fixed entities not handled yet" }
    }
    val nudgeDistances = tileBbox(-nudgeRange, -nudgeRange, nudgeRange + 1, nudgeRange + 1)
        .map { it.topLeftCorner() }

    @Suppress("UNCHECKED_CAST") val inserters =
        placements.filter { it.prototype is InserterPrototype } as List<EntityPlacement<InserterPrototype>>


    val inserterLinks = inserters.associateWith { inserter ->
        val pickup = inserter.globalPickupPosition()
        val dropoff = inserter.globalInsertPosition()
        val pickupEntity = placements.getAtPoint(pickup).firstOrNull()
        val dropoffEntity = placements.getAtPoint(dropoff).firstOrNull()
        InserterLinks(pickupEntity, dropoffEntity)
    }
    this.removeAll(entitiesToNudge)

    val newPlacements = entitiesToNudge.associateWith { entity ->
        nudgeDistances.mapNotNull { nudgeVec ->
            val newPosition = entity.position + nudgeVec
            val cost = if (nudgeVec.isZero()) 0.0 else nudgeCost
            addPlacementIfPossible(entity.prototype, newPosition, entity.direction, cost)
        }.also { placements ->
            require(placements.isNotEmpty()) { "No valid placements for $entity" }
            cp.addExactlyOneLenient(placements.map { it.selected })
        }
    }

    fun enforceInserterLink(
        inserter: EntityPlacement<InserterPrototype>,
        linkPosition: Position,
        linkEntities: Set<EntityPlacement<*>>?,
    ) {
        val atPos = placements.getAtPoint(linkPosition)
        if (linkEntities.isNullOrEmpty()) {
            val atPosSelected = atPos.mapTo(ArrayList()) { it.selected.not() }
            if (atPosSelected.isNotEmpty())
                cp.addBoolAndLenient(atPosSelected, inserter.selected)
        } else {
            val validSelected = atPos.filter { it in linkEntities }
                .mapTo(ArrayList()) { it.selected }
            cp.addAtLeastOne(validSelected).onlyEnforceIf(inserter.selected)
        }
    }

    for ((inserter, links) in inserterLinks) {
        val pickupEntities =
            links.pickupEntity?.let { newPlacements[it]?.toSet() ?: setOf(it) }
        val insertEntities =
            links.insertEntity?.let { newPlacements[it]?.toSet() ?: setOf(it) }
        for (placement in newPlacements[inserter]!!) {
            @Suppress("UNCHECKED_CAST")
            placement as EntityPlacement<InserterPrototype>
            enforceInserterLink(placement, placement.globalPickupPosition(), pickupEntities)
            enforceInserterLink(placement, placement.globalInsertPosition(), insertEntities)
        }
    }
}
*/
