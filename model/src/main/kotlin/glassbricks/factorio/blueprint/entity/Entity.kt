package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.CollisionMask
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.EntityWithOwnerPrototype
import glassbricks.factorio.blueprint.prototypes.effectiveCollisionMask
import kotlinx.serialization.json.JsonObject

public fun computeCollisionBox(
    prototype: EntityPrototype,
    position: Position,
    direction: Direction,
): BoundingBox = (prototype.collision_box?.rotateCardinal(direction)?.translate(position)
    ?: BoundingBox(position, position))

public interface EntityInfo : Spatial {
    public val prototype: EntityPrototype
    public val direction: Direction
    public val type: String get() = prototype.type
    public val name: String get() = prototype.name
    override val collisionMask: CollisionMask get() = prototype.effectiveCollisionMask
}

/**
 * Only holds the spatial properties of an entity, used for e.g. collision checks.
 */
public open class BasicEntityInfo<out P : EntityPrototype>(
    public override val prototype: P,
    public override val position: Position,
    public override val direction: Direction = Direction.North
) : EntityInfo {
    override val collisionBox: BoundingBox get() = computeCollisionBox(prototype, position, direction)
    override val isSimpleCollisionBox: Boolean get() = true
}

public fun EntityInfo.matches(other: EntityInfo): Boolean {
    return prototype == other.prototype && position == other.position && direction == other.direction
}

public interface Entity : EntityInfo {
    override val collisionBox: BoundingBox

    public override var position: Position
    public override var direction: Direction
    public var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson

    /** Creates a copy of this entity, but without any connections to other entities. */
    public fun copyIsolated(): Entity
}

public abstract class BaseEntity(json: EntityJson) : Entity {
    override var position: Position = json.position
        set(value) {
            if (field != value) cachedCollisionBox = null
            field = value
        }
    override var direction: Direction = json.direction
        set(value) {
            if (field != value) cachedCollisionBox = null
            field = value
        }
    override var tags: JsonObject? = json.tags

    private var cachedCollisionBox: BoundingBox? = null
    override val collisionBox: BoundingBox
        get() = cachedCollisionBox ?: computeCollisionBox(prototype, position, direction).also {
            cachedCollisionBox = it
        }

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

    override fun toString(): String = "${this::class.simpleName}(name=$name, position=$position, direction=$direction)"
}

/**
 * Any entity with no extra properties, and so does not fit into any other specialized entity classes.
 *
 * @param P The prototype type of this entity.
 */
public class BasicEntity<out P : EntityPrototype>(
    override val prototype: P,
    json: EntityJson,
) : BaseEntity(json) {
    override fun exportToJson(json: EntityJson) {}
    override fun copyIsolated(): BasicEntity<P> = BasicEntity(prototype, toDummyJson())
}
