package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.prototypes.CollisionMask
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.effectiveCollisionMask
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition

/**
 * Only holds the spatial properties of an entity, used for e.g. collision checks.
 */
public fun Entity<*>.matches(other: Entity<*>): Boolean {
    return prototype == other.prototype && position == other.position && direction == other.direction
}

public interface Entity<out P : EntityPrototype> : Spatial {
    public val prototype: P
    public val direction: Direction
    public val type: String get() = prototype.type
    public val name: String get() = prototype.name
    override val collisionMask: CollisionMask get() = prototype.effectiveCollisionMask
}

public inline fun <reified P : EntityPrototype> Entity<*>.asEntity(): Entity<P>? {
    @Suppress("UNCHECKED_CAST")
    if (prototype is P) return this as Entity<P>
    return null
}


public inline fun <reified P : EntityPrototype> Entity<*>.castEntity(): Entity<P> {
    prototype as P
    @Suppress("UNCHECKED_CAST")
    return this as Entity<P>
}

public fun computeCollisionBox(
    prototype: EntityPrototype,
    position: Position,
    direction: Direction,
): BoundingBox =
    (prototype.collision_box?.rotateCardinal(direction)?.translate(position) ?: BoundingBox(position, position))

/**
 * Only holds the spatial properties of an entity, used for e.g. collision checks.
 */
public open class BasicEntity<out P : EntityPrototype>(
    public override val prototype: P,
    public override val position: Position,
    public override val direction: Direction = Direction.North,
) : Entity<P> {
    override val collisionBox: BoundingBox get() = computeCollisionBox(prototype, position, direction)
    override val isSimpleCollisionBox: Boolean get() = true
}

public fun <P : EntityPrototype> P.basicPlacedAtTile(
    position: TilePosition,
    direction: Direction = Direction.North,
): BasicEntity<P> =
    BasicEntity(this, this.tileSnappedPosition(position), direction)
