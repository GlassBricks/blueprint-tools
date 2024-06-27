package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.EntityNumber
import glassbricks.factorio.blueprint.Position
import kotlinx.serialization.json.JsonObject

public abstract class BaseEntity(source: EntityProps) : Entity {
    override var position: Position = source.position
    override var direction: Direction = source.direction
    override var tags: JsonObject? = source.tags

    private fun createEntityJson(entityNumber: EntityNumber): EntityJson = EntityJson(
        entity_number = entityNumber,
        name = prototype.name,
        position = position,
        direction = direction,
    )


    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson {
        val json = createEntityJson(entityNumber)
        configure(json)
        return json
    }
    protected abstract fun configure(json: EntityJson)

    override fun configureConnections(json: EntityJson, entityAssignment: Map<Entity, EntityJson>) {
        // no-op; overridden in subclasses if needed
    }
    
    public abstract fun copy(): BaseEntity
}
