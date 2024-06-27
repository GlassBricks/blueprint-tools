package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.EntityNumber
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// todo: make this implement every trait
public class UnknownEntity private constructor(
    override val prototype: EntityWithOwnerPrototype,
    public val source: EntityJson,
) : Entity, CircuitConnectable2 by CircuitConnectable2Mixin() {
    public constructor(
        prototype: EntityWithOwnerPrototype,
        source: EntityProps,
    ) : this(prototype, source.copyToJson())
    
    public constructor(
        name: String,
        position: Position,
        direction: Direction = Direction.North,
    ): this(UnknownPrototype(name), BasicEntityProps(name, position, direction))

    override var position: Position by this.source::position
    override var tags: JsonObject? by this.source::tags
    override var direction: Direction by this.source::direction

    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson = source.copy(
        entity_number = entityNumber,
        connections = null,
        neighbours = null,
    )
}

public class UnknownPrototype(override val name: String) : EntityWithOwnerPrototype() {
    init {
        this.fakeInit(
            JsonObject(mapOf("type" to JsonPrimitive("unknown"))),
            eager = true
        )
    }
}
