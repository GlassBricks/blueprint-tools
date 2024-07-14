package glassbricks.factorio.blueprint


/**
 * A data structure to store 2d entities and query them by their position.
 */
public interface SpatialDataStructure<T : Spatial> : Collection<T> {

    /** Gets all entities that intersect with the given area. */
    public fun getInArea(area: BoundingBox): Sequence<T>

    /** Gets all entities that intersect with the given tile area. */
    public fun getInArea(area: TileBoundingBox): Sequence<T>

    /** Gets all entities that intersect with the given tile. */
    public fun getInTile(tile: TilePosition): Sequence<T>

    /** Gets all entities that intersect with the given point. */
    public fun getAtPoint(position: Position): Sequence<T>

    /**
     * Gets all entities that are within the given circle.
     * Note that this uses [Spatial.position] instead of [Spatial.collisionBox] to test for distance,
     * (as is done in Factorio).
     */
    public fun getPosInCircle(center: Position, radius: Double): Sequence<T>

    /** Gets all entities that collide with the given entity. */
    public fun getColliding(other: Spatial): Sequence<T> =
        getInArea(other.collisionBox).filter { it collidesWith other }
}

public interface MutableSpatialDataStructure<T : Spatial> : SpatialDataStructure<T>, MutableCollection<T>


// todo: replace this with DI?
public fun <T : Spatial> DefaultSpatialDataStructure(): MutableSpatialDataStructure<T> =
    SpatialTileHashMap()
