package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.prototypes.CollisionMask


/**
 * Represents something that has a bounding box in the world.
 */
public interface Spatial {
    /**
     * The "broad" collision bounding box.
     *
     * For axis-aligned bounding boxes, this is exactly the bounding box.
     *
     * The only collision box not axis-aligned in Factorio is curved rails.
     */
    public val boundingBox: BoundingBox
    public val collisionMask: CollisionMask

    /**
     * True if [boundingBox] is exactly the collision box of this entity; true for most entities.
     *
     * If overriding this to false, you must override [collidesWith] as well.
     */
    public val boundingBoxIsSimple: Boolean get() = true

    public infix fun collidesWith(other: Spatial): Boolean {
        if (!other.boundingBoxIsSimple) return other collidesWith this
        return boundingBox intersects other.boundingBox && collisionMask collidesWith other.collisionMask
    }
}
