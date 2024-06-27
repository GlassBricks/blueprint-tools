package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject

public typealias EntityJson = glassbricks.factorio.blueprint.Entity
public typealias BlueprintJson = Blueprint

public interface Entity : EntityProps {
    public val prototype: EntityWithOwnerPrototype
    public override val name: String get() = prototype.name
    public val type: String get() = prototype.type

    public override var position: Position
    public override var direction: Direction
    public override var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson
    public fun configureConnections(
        json: EntityJson,
        entityAssignment: Map<Entity, EntityJson>,
    )
}

public interface WithSchedule : Entity {
    public val schedule: List<ScheduleRecord>
}

public interface EntityProps {
    public val name: String
    public val position: Position
    public val direction: Direction
    public val tags: JsonObject?
}

public class BasicEntityProps(
    public override val name: String,
    public override val position: Position,
    public override val direction: Direction = Direction.North,
    public override var tags: JsonObject? = null,
) : EntityProps

public class FromJson(
    public val json: EntityJson,
    public val originalBlueprint: BlueprintJson? = null,
) : EntityProps {
    public override val name: String get() = json.name
    public override val position: Position get() = json.position
    public override val direction: Direction get() = json.direction
    public override var tags: JsonObject? = json.tags

    public fun getSchedule(): List<ScheduleRecord>? =
        originalBlueprint?.schedules
            ?.firstOrNull { it.locomotives.any { number -> number == json.entity_number } }
            ?.schedule
}

internal fun EntityProps.asJson(): EntityJson? = (this as? FromJson)?.json
internal fun EntityProps.asFromJson(): FromJson? = this as? FromJson
internal fun EntityProps.basicToJson(): EntityJson = asJson()?.copy(
    connections = null,
    neighbours = null,
) ?: EntityJson(
    entity_number = EntityNumber(0),
    name = name,
    position = position,
    direction = direction,
    tags = tags,
)
