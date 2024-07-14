package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Spatial
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.CollisionMask
import glassbricks.factorio.blueprint.prototypes.EntityWithOwnerPrototype
import kotlinx.serialization.json.JsonObject

public interface Entity : Spatial {
    public val prototype: EntityWithOwnerPrototype
    public val type: String get() = prototype.type
    public val name: String get() = prototype.name

    override val collisionMask: CollisionMask get() = prototype.collision_mask ?: CollisionMask.EMPTY
    override val collisionBox: BoundingBox

    public override var position: Position
    public var direction: Direction
    public var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson

    public fun copyIsolated(): Entity
}

public abstract class BaseEntity(json: EntityJson) : Entity {
    override var position: Position = json.position
        set(value) {
            if (field != value) cachedBoundingBox = null
            field = value
        }
    override var direction: Direction = json.direction
        set(value) {
            if (field != value) cachedBoundingBox = null
            field = value
        }
    override var tags: JsonObject? = json.tags

    protected var cachedBoundingBox: BoundingBox? = null
    override val collisionBox: BoundingBox
        get() = cachedBoundingBox ?: (prototype.collision_box?.rotateCardinal(direction)?.translate(position)
            ?: BoundingBox(position, position))
            .also { cachedBoundingBox = it }

    public override val collisionMask: CollisionMask
        get() = prototype.collision_mask ?: CollisionMask.DEFAULT_MASKS[prototype.type]!!

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

    protected fun toDummyJson(): EntityJson = toJsonIsolated(EntityNumber(1))
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
    override fun exportToJson(json: EntityJson) {}
    override fun copyIsolated(): BasicEntity<P> = BasicEntity(prototype, toDummyJson())
}
