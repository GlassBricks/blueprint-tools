package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Spatial
import glassbricks.factorio.blueprint.prototypes.CollisionMask

class SimpleSpatial(
    override val position: Position,
    val localCollisionBox: BoundingBox = BoundingBox.around(Position.ZERO, 0.5)
) : Spatial {
    override val collisionBox: BoundingBox
        get() = localCollisionBox.translate(position)
    override val collisionMask: CollisionMask
        get() = CollisionMask.PLAIN_OBJECT_MASK
}
