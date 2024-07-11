package glassbricks.factorio.blueprint.prototypes

import kotlin.String

public interface PrototypeBase {
  /**
   * Specifies the kind of prototype this is.
   *
   * For a list of all types used in vanilla, see [data.raw](https://wiki.factorio.com/Data.raw).
   */
  public val type: String

  /**
   * Unique textual identification of the prototype. May not contain a dot, nor exceed a length of
   * 200 characters.
   *
   * For a list of all names used in vanilla, see [data.raw](https://wiki.factorio.com/Data.raw).
   */
  public val name: String
}

public interface EntityPrototype {
  /**
   * Specification of the entity collision boundaries. Empty collision box means no collision and is
   * used for smoke, projectiles, particles, explosions etc.
   *
   * The `{0,0}` coordinate in the collision box will match the entity position. It should be near
   * the center of the collision box, to keep correct entity drawing order. The bounding box must
   * include the `{0,0}` coordinate.
   *
   * Note, that for buildings, it is customary to leave 0.1 wide border between the edge of the tile
   * and the edge of the building, this lets the player move between the building and electric
   * poles/inserters etc.
   */
  public val collision_box: BoundingBox?

  /**
   * Two entities can collide only if they share a layer from the collision mask.
   */
  public val collision_mask: CollisionMask?
}
