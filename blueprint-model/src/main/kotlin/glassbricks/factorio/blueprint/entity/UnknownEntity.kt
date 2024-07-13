package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.EntityWithOwnerPrototype
import glassbricks.factorio.blueprint.prototypes.SimpleEntityWithForcePrototype
import glassbricks.factorio.blueprint.prototypes.SimpleEntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Unknown

public class UnknownEntity(
    override val prototype: UnknownPrototype,
    json: EntityJson,
) : Entity,
    CableConnectionPoint,
    CircuitConnectable2,
    WithColor,
    WithBar,
    WithItemFilters {
    public val json: EntityJson = json.deepCopy()

    override var position: Position by json::position
    override var tags: JsonObject? by json::tags
    override var direction: Direction by json::direction
    override var color: Color? by json::color
    override var bar: Int? by json::bar
    override val filters: Array<String?> = json.filters.toFilters(128)

    override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this, CircuitID.First)
    override val connectionPoint2: CircuitConnectionPoint = CircuitConnectionPoint(this, CircuitID.Second)
    override val controlBehavior: ControlBehavior? get() = null

    override val entity: Entity get() = this

    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson = json.copy(
        entity_number = entityNumber,
        connections = null,
        neighbours = null,
        filters = this.getFiltersAsList(),
    )
}

public fun UnknownEntity(
    name: String,
    position: Position,
    direction: Direction = Direction.North,
): UnknownEntity {
    return UnknownEntity(UnknownPrototype(name), EntityJson(EntityNumber(1), name, position, direction))
}

private fun EntityJson.getJson(): EntityJson {
    val json = this
    return json.deepCopy()
}


public class UnknownPrototype(name: String) : SimpleEntityWithOwnerPrototype() {
    init {
        this.name = name
        this.type = "unknown"
    }
}
