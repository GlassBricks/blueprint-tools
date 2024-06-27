package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.EntityNumber
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public class UnknownEntity(
    override val prototype: EntityWithOwnerPrototype,
    source: EntityProps,
) : Entity {
    public val source: EntityJson = source.basicToJson()
    override var position: Position by this.source::position
    override var tags: JsonObject? by this.source::tags
    override var direction: Direction by this.source::direction

    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson {
        return source.copy(
            entity_number = entityNumber,
            connections = null,
            neighbours = null,
        )
    }

    override fun configureConnections(json: EntityJson, entityAssignment: Map<Entity, EntityJson>) {
        // noop
    }
}

public class UnknownPrototype(
    override val name: String,
) : EntityWithOwnerPrototype() {
    init {
        this.fakeInit(
            JsonObject(mapOf("type" to JsonPrimitive("unknown"))),
            eager = true
        )
    }
}
