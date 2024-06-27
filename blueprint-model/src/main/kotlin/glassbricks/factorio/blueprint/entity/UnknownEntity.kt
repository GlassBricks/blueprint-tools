package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public class UnknownEntity internal constructor(
    override val prototype: EntityWithOwnerPrototype,
    init: EntityInit<UnknownEntity>,
) : Entity,
    CircuitConnectable2,
    WithColor,
    WithBar,
    WithItemFilters {
    public val json: EntityJson = init.getJson()

    override var position: Position by json::position
    override var tags: JsonObject? by json::tags
    override var direction: Direction by json::direction
    override var color: Color? by json::color
    override var bar: Int? by json::bar
    override val filters: Array<String?> = init.self?.filters ?: json.filters.toFilters(128)

    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this, CircuitID.First)
    override val connectionPoint2: CircuitConnectionPoint = CircuitConnectionPoint(this, CircuitID.Second)

    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson = json.copy(
        entity_number = entityNumber,
        connections = null,
        neighbours = null,
        filters = this.getFiltersAsList(),
    )

    override fun copy(): Entity = UnknownEntity(prototype, copyInit(this))
}

public fun UnknownEntity(
    name: String,
    position: Position,
    direction: Direction = Direction.North,
): UnknownEntity {
    return UnknownEntity(UnknownPrototype(name), propInit(name, position, direction))
}

private fun EntityInit<UnknownEntity>.getJson(): EntityJson {
    val json = this.self?.json
        ?: this.json
        ?: EntityJson(
            entity_number = EntityNumber(1),
            name = this.name,
            position = this.position,
            direction = this.direction,
            tags = this.tags,
        )
    return json.deepCopy()
}


public class UnknownPrototype(override val name: String) : EntityWithOwnerPrototype() {
    init {
        this.fakeInit(
            JsonObject(mapOf("type" to JsonPrimitive("unknown"))),
            eager = true
        )
    }
}
