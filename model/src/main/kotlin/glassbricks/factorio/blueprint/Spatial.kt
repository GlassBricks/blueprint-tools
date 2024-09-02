package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.prototypes.CollisionMask


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
     * If overriding this to false, you should override [intersects] as well.
     */
    public val isSimpleCollisionBox: Boolean get() = true

    public infix fun intersects(area: BoundingBox): Boolean = collisionBox intersects area
    public infix fun collidesWith(other: Spatial): Boolean {
        if (!other.isSimpleCollisionBox) return other collidesWith this
        return collisionMask collidesWith other.collisionMask && this.intersects(other.collisionBox)
    }
}
