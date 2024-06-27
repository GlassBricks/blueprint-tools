package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Direction
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.json.Position
import kotlinx.serialization.json.JsonObject

public abstract class BaseEntity
internal constructor(init: EntityProps) : Entity {
    override var position: Position = init.position
    override var direction: Direction = init.direction
    override var tags: JsonObject? = init.tags

    private fun createEntityJson(entityNumber: EntityNumber): EntityJson = EntityJson(
        entity_number = entityNumber,
        name = prototype.name,
        position = position,
        direction = direction,
    )

    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson {
        return createEntityJson(entityNumber)
            .apply { exportToJson(this) }
    }
    protected abstract fun exportToJson(json: EntityJson)
}
