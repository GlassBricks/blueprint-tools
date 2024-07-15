package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.entity.copyEntitiesWithConnections


public typealias EntityMap = SpatialDataStructure<Entity>
public typealias MutableEntityMap = MutableSpatialDataStructure<Entity>

public fun <T : Entity> SpatialDataStructure<T>.copyEntities(): MutableSpatialDataStructure<T> {
    return DefaultSpatialDataStructure<T>()
        .also {
            it.addAll(copyEntitiesWithConnections(this).values)
        }
}
