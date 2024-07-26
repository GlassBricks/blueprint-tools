package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.prototypes.InserterPrototype


public fun Entity<InserterPrototype>.defaultInsertPosition(): Position {
    return position + prototype.insert_position.rotate(direction).toPosition()
}

public fun Entity<InserterPrototype>.defaultPickupPosition(): Position {
    return position + prototype.pickup_position.rotate(direction).toPosition()
}
