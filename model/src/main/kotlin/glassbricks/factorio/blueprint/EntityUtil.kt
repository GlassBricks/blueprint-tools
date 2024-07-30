package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.entity.Inserter
import glassbricks.factorio.blueprint.prototypes.InserterPrototype


public fun Entity<InserterPrototype>.globalInsertPosition(): Position {
    val relativeInsertPosition = (this as? Inserter)?.pickupPosition?.toVector() ?: prototype.insert_position
    return position + relativeInsertPosition.rotate(direction).toPosition()
}

public fun Inserter.globalInsertPosition(): Position {
    val relativeInsertPosition = pickupPosition?.toVector() ?: prototype.insert_position
    return position + relativeInsertPosition.rotate(direction).toPosition()
}

public fun Entity<InserterPrototype>.globalPickupPosition(): Position {
    val relativePickupPosition = (this as? Inserter)?.pickupPosition?.toVector() ?: prototype.pickup_position
    return position + relativePickupPosition.rotate(direction).toPosition()
}

public fun Inserter.globalPickupPosition(): Position {
    val relativePickupPosition = pickupPosition?.toVector() ?: prototype.pickup_position
    return position + relativePickupPosition.rotate(direction).toPosition()
}
