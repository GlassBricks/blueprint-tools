package glassbricks.factorio.blueprint.prototypes

/**
 * Abstract base of all entities in the game. Entity is nearly everything that can be on the map
 * (except tiles).
 *
 * For in game script access to entity, take a look at [LuaEntity](runtime:LuaEntity).
 */
public interface EntityPrototype : PrototypeBase {
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

  public val flags: EntityPrototypeFlags?

  /**
   * Supported values are 1 (for 1x1 grid) and 2 (for 2x2 grid, like rails).
   *
   * Internally forced to be `2` for [RailPrototype](prototype:RailPrototype),
   * [RailRemnantsPrototype](prototype:RailRemnantsPrototype) and
   * [TrainStopPrototype](prototype:TrainStopPrototype).
   */
  public val build_grid_size: UByte?

  /**
   * Item that when placed creates this entity. Determines which item is picked when "Q" (smart
   * pipette) is used on this entity. Determines which item and item amount is needed in a blueprint of
   * this entity and to revive a ghost of this entity.
   *
   * The item count specified here can't be larger than the stack size of that item.
   */
  public val placeable_by: ItemOrArray<ItemToPlace>?

  /**
   * Used to determine how the center of the entity should be positioned when building (unless the
   * off-grid [flag](prototype:EntityPrototypeFlags) is specified).
   *
   * When the tile width is odd, the center will be in the center of the tile, when it is even, the
   * center is on the tile transition.
   */
  public val tile_width: UInt?

  public val tile_height: UInt?
}

/**
 * The abstract base for prototypes. PrototypeBase defines the common features of prototypes, such
 * as localization and order.
 */
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

/**
 * Every entry in the array is a specification of one layer the object collides with or a special
 * collision option. Supplying an empty table means that no layers and no collision options are set.
 *
 * The default collision masks of all entity types can be found
 * [here](prototype:EntityPrototype::collision_mask). The base game provides common collision mask
 * functions in a Lua file in the core
 * [lualib](https://github.com/wube/factorio-data/blob/master/core/lualib/collision-mask-util.lua).
 *
 * Supplying an empty array means that no layers and no collision options are set.
 *
 * The three options in addition to the standard layers are not collision masks, instead they
 * control other aspects of collision.
 */
public typealias CollisionMask = List<String>

public enum class EntityPrototypeFlagsValue {
  /**
   * Can't be rotated before or after placing.
   */
  `not-rotatable`,
  /**
   * Determines the default force when placing entities in the map editor and using the *AUTO*
   * option for the force.
   */
  `placeable-neutral`,
  /**
   * Determines the default force when placing entities in the map editor and using the *AUTO*
   * option for the force.
   */
  `placeable-player`,
  /**
   * Determines the default force when placing entities in the map editor and using the *AUTO*
   * option for the force.
   */
  `placeable-enemy`,
  /**
   * Refers to the fact that most entities are placed on an invisible 'grid' within the world,
   * entities with this flag do not have to line up with this grid (like trees and land-mines).
   */
  `placeable-off-grid`,
  /**
   * Makes it possible for the biter AI to target the entity as a distraction in distraction mode
   * [by_anything](runtime:defines.distraction.by_anything). Makes it possible to blueprint,
   * deconstruct, and repair the entity (can be turned off again using the specific flags). Enables
   * smoke to be created automatically when building the entity. If the entity does not have
   * [EntityPrototype::map_color](prototype:EntityPrototype::map_color) set, this flag makes the entity
   * appear on the map/minimap with the default color specified in the
   * [UtilityConstants](prototype:UtilityConstants).
   */
  `player-creation`,
  /**
   * Uses 45 degree angle increments when selecting direction.
   */
  `building-direction-8-way`,
  /**
   * Used to automatically detect the proper direction, if possible. Used by base with the pump,
   * train stop, and train signal.
   */
  `filter-directions`,
  /**
   * Fast replace will not apply when building while moving.
   */
  `fast-replaceable-no-build-while-moving`,
  /**
   * This is used to specify that the entity breathes air, and so is affected by poison (currently
   * [poison capsules](https://wiki.factorio.com/Poison_capsule) are the only source).
   */
  `breaths-air`,
  /**
   * Used to specify that the entity can not be 'healed' by repair-packs (or construction robots
   * with repair packs)
   */
  `not-repairable`,
  /**
   * The entity does not get drawn on the map.
   */
  `not-on-map`,
  /**
   * The entity can not be deconstructed.
   */
  `not-deconstructable`,
  /**
   * The entity can not be used in blueprints.
   */
  `not-blueprintable`,
  /**
   * Hides the entity from the bonus GUI (button above the map) and from the made in property of
   * recipe tooltips.
   */
  hidden,
  /**
   * Hides the alt-info of an entity in alt-mode, for example the recipe icon.
   */
  `hide-alt-info`,
  /**
   * Do not fast replace over other entity types when building while moving.
   */
  `fast-replaceable-no-cross-type-while-moving`,
  `no-gap-fill-while-building`,
  /**
   * Do not apply fire stickers to the entity.
   */
  `not-flammable`,
  /**
   * Prevents inserters and loaders from taking items from this entity.
   */
  `no-automated-item-removal`,
  /**
   * Prevents inserters and loaders from inserting items into this entity.
   */
  `no-automated-item-insertion`,
  /**
   * This flag does nothing when set in the data stage because it gets overridden by
   * [EntityPrototype::allow_copy_paste](prototype:EntityPrototype::allow_copy_paste). Thus, it must be
   * set on the entity via that property.
   */
  `no-copy-paste`,
  /**
   * Disallows selection of the entity even when a selection box is specified for other reasons. For
   * example, selection boxes are used to determine the size of outlines to be shown when highlighting
   * entities inside electric pole ranges. This flag does nothing when set in the data stage because it
   * gets overridden by
   * [EntityPrototype::selectable_in_game](prototype:EntityPrototype::selectable_in_game). Thus, it
   * must be set on the entity via that property.
   */
  `not-selectable-in-game`,
  /**
   * The entity can't be selected by the [upgrade
   * planner](https://wiki.factorio.com/Upgrade_planner).
   */
  `not-upgradable`,
  /**
   * The entity is not shown in the kill statistics.
   */
  `not-in-kill-statistics`,
  /**
   * The entity is not shown in the made in property of recipe tooltips.
   */
  `not-in-made-in`,
}

/**
 * An array containing the following values.
 *
 * If an entity is a [building](runtime:LuaEntityPrototype::is_building) and has the
 * `"player-creation"` flag set, it is considered for multiple enemy/unit behaviors:
 *
 * - Autonomous enemy attacks (usually triggered by pollution) can only attack within chunks that
 * contain at least one entity that is both a building and a player-creation.
 *
 * - Enemy expansion considers entities that are both buildings and player-creations as "enemy"
 * entities that may block expansion.
 */
public typealias EntityPrototypeFlags = List<EntityPrototypeFlagsValue>

/**
 * The name of an [ItemPrototype](prototype:ItemPrototype).
 */
public typealias ItemID = String

/**
 * Item that when placed creates this entity/tile.Item that when placed creates this entity/tile.
 */
public interface ItemToPlace {
  /**
   * How many items are used to place one of this entity/tile. Can't be larger than the stack size
   * of the item.
   */
  public val count: UInt

  /**
   * The item used to place this entity/tile.
   */
  public val item: ItemID
}
