package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.entity.TileHashMap


/**
 * A data structure to store 2d entities and query them by their position.
 */
public interface SpatialDataStructure<out T : Spatial> : Collection<T> {

    /** Gets all entities that intersect with the given area. */
    public fun getInArea(area: BoundingBox): Sequence<T>

    /** Gets all entities that intersect with the given tile area. */
    public fun getInArea(area: TileBoundingBox): Sequence<T>

    /** Gets all entities that intersect with the given tile. */
    public fun getInTile(tile: TilePosition): Sequence<T>

    /** Gets all entities that intersect with the given point. */
    public fun getIntersectingPosition(position: Position): Sequence<T>

    /**
     * Gets all entities that are within the given circle.
     * Note that this uses [Spatial.position] instead of [Spatial.collisionBox] to test for distance,
     * (as is done in Factorio).
     */
    public fun getPosInCircle(center: Position, radius: Double): Sequence<T>

    /** Gets all entities that collide with the given entity. */
    public fun getColliding(other: Spatial): Sequence<T>
    public fun tileIsOccupied(tile: TilePosition): Boolean = getInTile(tile).any()

    /**
     * Iterates over all tiles in the data structure that might contain entities.
     */
    public fun occupiedTiles(): Iterable<TilePosition>

    /**
     * Gets the minimal size bounding box to enclose all entities.
     */
    public fun enclosingBox(): BoundingBox = getEnclosingBox(map { it.collisionBox })

    public fun enclosingTileBox(): TileBoundingBox = enclosingBox().roundOutToTileBbox()
}

public interface MutableSpatialDataStructure<T : Spatial> : SpatialDataStructure<T>, MutableCollection<T> {
    public fun addIfNotColliding(entity: T): Boolean = getColliding(entity).none() && add(entity)
}


public fun <T : Spatial> DefaultSpatialDataStructure(): MutableSpatialDataStructure<T> =
    TileHashMap()

public fun <T : Spatial> DefaultSpatialDataStructure(items: Iterable<T>): MutableSpatialDataStructure<T> =
    TileHashMap<T>().also { it.addAll(items) }

public fun <T : Entity<*>> SpatialDataStructure<T>.findMatching(other: Entity<*>): T? =
    getInTile(other.position.occupiedTile())
        .find { it.matches(other) }
