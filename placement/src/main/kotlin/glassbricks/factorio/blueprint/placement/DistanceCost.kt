package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.Position


fun EntityPlacementModel.addDistanceCostFrom(position: Position, scale: Double = 1e-6) {
    for (entity in placements) if (entity is OptionalEntityPlacement<*>) {
        entity.cost += scale * entity.position.distanceTo(position)
    }
}
