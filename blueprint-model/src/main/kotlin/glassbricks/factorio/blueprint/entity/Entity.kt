package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Direction
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject

public interface EntityProps {
    public val name: String
    public val position: Position
    public val direction: Direction
    public val tags: JsonObject?
}

public interface Entity : EntityProps {
    public val prototype: EntityWithOwnerPrototype
    public val type: String get() = prototype.type

    public override val name: String get() = prototype.name
    public override var position: Position
    public override var direction: Direction
    public override var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson
}

internal fun EntityJson.deepCopy() = copy() // todo: actually deep copy

public abstract class BaseEntity(json: EntityJson) : Entity {
    override var position: Position = json.position
    override var direction: Direction = json.direction
    override var tags: JsonObject? = json.tags

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

/**
 * Any entity with no extra properties, and so does not fit into any other specialized entity classes.
 *
 * @param P The prototype type of this entity.
 */
public class BasicEntity<out P : EntityWithOwnerPrototype>(
    override val prototype: P,
    json: EntityJson,
) : BaseEntity(json) {
    override fun exportToJson(json: EntityJson) {
    }
}
