package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.entity.EntityInfo
import glassbricks.factorio.blueprint.entity.basicEntityJson
import glassbricks.factorio.blueprint.entity.createEntity


public interface PlacementOptions {
    public val options: List<EntityPlacement>
}

/**
 * An option for placement, in optimization.
 *
 * @property cost The cost of using this option.
 */
public interface EntityPlacement : EntityInfo {
    public val cost: Double
}

public fun EntityPlacement.toEntity(): Entity = createEntity(prototype, basicEntityJson(prototype.name, position, direction))
