package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.CollisionMask
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import kotlinx.serialization.json.JsonObject

public interface BlueprintEntity : Entity<EntityPrototype> {
    override val collisionBox: BoundingBox

    public override var position: Position
    public override var direction: Direction
    public var tags: JsonObject?

    public fun toJsonIsolated(entityNumber: EntityNumber): EntityJson

    /** Creates a copy of this entity, but without any connections to other entities. */
    public fun copyIsolated(): BlueprintEntity
}

public abstract class BaseEntity(json: EntityJson) : BlueprintEntity {
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
        get() = cachedCollisionBox ?: this.computeThisCollisionBox(
            prototype,
            position,
            direction,
        ).also {
            cachedCollisionBox = it
        }

    protected open fun computeThisCollisionBox(
        prototype: EntityPrototype,
        position: Position,
        direction: Direction,
    ): BoundingBox =
        computeCollisionBox(prototype, position, direction)

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

    protected fun jsonForCopy(): EntityJson = toJsonIsolated(EntityNumber(1))

    override fun toString(): String = "${this::class.simpleName}(name=$name, position=$position, direction=$direction)"
}

/**
 * Any entity with no extra properties, and so does not fit into any other specialized entity classes.
 *
 * @param P The prototype type of this entity.
 */
public class BasicBpEntity<out P : EntityPrototype>(
    override val prototype: P,
    json: EntityJson,
) : BaseEntity(json) {
    override fun exportToJson(json: EntityJson) {}
    override fun copyIsolated(): BasicBpEntity<P> = BasicBpEntity(prototype, jsonForCopy())
}
