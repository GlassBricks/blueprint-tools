package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.entity.computeCollisionBox
import glassbricks.factorio.blueprint.prototypes.CollisionMask
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.effectiveCollisionMask


/**
 * Represents something with a 2d collision box (entities).
 */
public interface Spatial {
    /** Position used for distance checks. The bounding box should contain this point. */
    public val position: Position

    /**
     * The "broad" collision bounding box.
     *
     * The entity's position must be within this box.
     *
     * For axis-aligned bounding boxes, this is exactly the bounding box.
     *
     * The only collision box not axis-aligned in Factorio is curved rails.
     */
    public val collisionBox: BoundingBox

    public val collisionMask: CollisionMask

    /**
     * True if [collisionBox] is exactly the collision box of this entity; true for most entities.
     *
     * If overriding this to false, you should override [collidesWith] as well.
     */
    public val isSimpleCollisionBox: Boolean get() = true

    public infix fun collidesWith(other: Spatial): Boolean {
        if (!other.isSimpleCollisionBox) return other collidesWith this
        return collisionBox intersects other.collisionBox && collisionMask collidesWith other.collisionMask
    }
}

/**
 * Returns all tiles that are overlapped by the collision box of this spatial.
 */
public fun Spatial.allOccupiedTiles(): Iterable<TilePosition> = collisionBox.roundOutToTileBbox().iterateTiles()

/**
 * Only holds the spatial properties of an entity, used for e.g. collision checks.
 */
public open class EntitySpatial<out T : EntityPrototype>(
    public val prototype: T,
    public override val position: Position,
    public val direction: Direction = Direction.North
) : Spatial {
    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)
    override val collisionMask: CollisionMask = prototype.effectiveCollisionMask
    override val isSimpleCollisionBox: Boolean get() = true
}
