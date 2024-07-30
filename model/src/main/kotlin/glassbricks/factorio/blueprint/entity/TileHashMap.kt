package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.*
import kotlin.math.abs
import kotlin.math.min

/**
 * An implementation of [SpatialDataStructure] that hashes entities into the tiles they occupy.
 */
public class TileHashMap<T : Spatial> :
    AbstractMutableCollection<T>(), MutableSpatialDataStructure<T> {
    private val entities = hashSetOf<T>()
    private val byTile = hashMapOf<TilePosition, HashSet<T>>()

    public fun byTile(): Map<TilePosition, Set<T>> = byTile

    override val size: Int get() = entities.size
    override fun contains(element: T): Boolean = entities.contains(element)

    override fun clear() {
        entities.clear()
        byTile.clear()
    }

    override fun add(element: T): Boolean {
        if (!entities.add(element)) return false
        addInTileMap(element)
        return true
    }

    private fun addInTileMap(element: T) {
        val tileBox = element.collisionBox.roundOutToTileBbox()
        for (tile in tileBox) {
            byTile.getOrPut(tile, ::hashSetOf).add(element)
        }
    }

    override fun remove(element: T): Boolean {
        if (!entities.remove(element)) return false
        removeInTileMap(element)
        return true
    }

    private fun removeInTileMap(element: T) {
        val tileBox = element.collisionBox.roundOutToTileBbox()
        for (tile in tileBox) {
            byTile[tile]?.let {
                it.remove(element)
                if (it.isEmpty()) byTile.remove(tile)
            }
        }
    }

    override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
        private val entitiesIterator = entities.iterator()
        private var current: T? = null

        override fun hasNext(): Boolean = entitiesIterator.hasNext()
        override fun next(): T = entitiesIterator.next().also { current = it }
        override fun remove() {
            entitiesIterator.remove()
            current?.let { removeInTileMap(it) }
        }
    }

    override fun getInTile(tile: TilePosition): Sequence<T> =
        byTile[tile]?.asSequence().orEmpty()

    override fun getInArea(area: TileBoundingBox): Sequence<T> =
        area.asSequence()
            .flatMap { getInTile(it) }
            .distinct()

    override fun getInArea(area: BoundingBox): Sequence<T> =
        getInArea(area.roundOutToTileBbox())
            .filter { area intersects it.collisionBox }

    override fun getAtPoint(position: Position): Sequence<T> =
        getInTile(position.occupiedTile())
            .filter { position in it.collisionBox }

    override fun getColliding(other: Spatial): Sequence<T> {
        val otherBox = other.collisionBox
        return getInArea(otherBox.roundOutToTileBbox())
            .filter { it collidesWith other }
    }

    override fun tileIsOccupied(tile: TilePosition): Boolean = tile in byTile

    override fun occupiedTiles(): Iterable<TilePosition> = byTile.keys

    override fun getPosInCircle(center: Position, radius: Double): Sequence<T> =
        BoundingBox.around(center, radius).roundOutToTileBbox().asSequence()
            .filter { canReachTile(it, center, radius) }
            .flatMap { tile ->
                getInTile(tile).filter {
                    tile == it.position.occupiedTile()
                            && center.squaredDistanceTo(it.position) <= radius * radius
                }
            }
}

private fun canReachTile(
    tile: TilePosition,
    center: Position,
    radius: Double
): Boolean {
    val xDist = min(abs(center.x - tile.x), abs(center.x - (tile.x + 1)))
    val yDist = min(abs(center.y - tile.y), abs(center.y - (tile.y + 1)))
    return xDist * xDist + yDist * yDist <= radius * radius
}
