package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.CircuitID
import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.ItemStackIndex
import glassbricks.factorio.blueprint.prototypes.SimpleEntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject

public class UnknownEntity(
    override val prototype: UnknownPrototype,
    json: EntityJson,
) : BlueprintEntity,
    CableConnectionPoint,
    CircuitConnectionPoint,
    CombinatorConnections,
    WithColor,
    WithInventory {
    public val json: EntityJson = json.deepCopy()
    override val collisionBox: BoundingBox get() = BoundingBox(position, position)

    override var position: Position by json::position
    override var tags: JsonObject? by json::tags
    override var direction: Direction by json::direction
    override var color: Color? by json::color
    override var bar: Int? by json::bar
    override val filters: Array<String?> = json.filters.toFilterArray(128)
    override val allowsFilters: Boolean get() = true
    override val inventorySize: ItemStackIndex get() = UShort.MAX_VALUE

    override val cableConnections: CableConnections = CableConnections(this)
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val input: CircuitConnectionPoint get() = this
    override val output: CircuitConnectionPoint = object : CircuitConnectionPoint {
        override val entity: BlueprintEntity get() = this@UnknownEntity
        override val circuitID: CircuitID get() = CircuitID.Second
        override val circuitConnections: CircuitConnections = CircuitConnections(this)
    }

    override val entity: BlueprintEntity get() = this

    override fun toJsonIsolated(entityNumber: EntityNumber): EntityJson = json.copy(
        entity_number = entityNumber,
        connections = null,
        neighbours = null,
        filters = this.filtersAsIndexList(),
    )

    override fun copyIsolated(): BlueprintEntity = UnknownEntity(prototype, toJsonIsolated(EntityNumber(0)))
}

public fun UnknownEntity(
    name: String,
    position: Position,
    direction: Direction = Direction.North,
): UnknownEntity {
    return UnknownEntity(
        UnknownPrototype(name),
        EntityJson(EntityNumber(1), name, position, direction)
    )
}

public class UnknownPrototype(name: String) : SimpleEntityWithOwnerPrototype() {
    init {
        this.name = name
        this.type = "unknown"
    }
}
