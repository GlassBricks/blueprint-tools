package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject

public interface Entity {
    public val prototype: EntityWithOwnerPrototype
    public val type: String get() = prototype.type
    public val name: String get() = prototype.name

    public var position: Position
    public var direction: Direction
    public var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson
}

internal fun EntityJson.deepCopy() = copy(
    control_behavior = control_behavior?.copy(),
)

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
            .also { exportToJson(it) }
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
