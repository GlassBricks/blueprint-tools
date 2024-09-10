package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.*
import kotlin.math.abs
import kotlin.math.min

/**
 * An implementation of [SpatialDataStructure] that hashes entities into the tiles they occupy.
 */
public open class TileHashMap<T : Spatial> :
    AbstractMutableCollection<T>(), MutableSpatialDataStructure<T> {
    protected val entities: HashSet<T> = hashSetOf()
    protected val byTile: HashMap<TilePosition, HashSet<T>> = hashMapOf()

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

    protected open fun addInTileMap(element: T) {
        val tileBox = element.collisionBox.roundOutToTileBbox()
        for (tile in tileBox) {
            if (element.isSimpleCollisionBox || element.intersects(tile.tileBoundingBox())) {
                byTile.getOrPut(tile, ::hashSetOf).add(element)
            }
        }
    }

    override fun remove(element: T): Boolean {
        if (!entities.remove(element)) return false
        removeInTileMap(element)
        return true
    }

    protected open fun removeInTileMap(element: T) {
        val tileBox = element.collisionBox.roundOutToTileBbox()
        for (tile in tileBox) {
            byTile[tile]?.let { set ->
                if (set.remove(element) && set.isEmpty) byTile.remove(tile)
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
        byTile[tile]
            .orEmpty()
            .asSequence()

    override fun getInArea(area: TileBoundingBox): Sequence<T> =
        area.asSequence()
            .flatMap { getInTile(it) }
            .distinct()

    override fun getInArea(area: BoundingBox): Sequence<T> =
        getInArea(area.roundOutToTileBbox())
            .filter { it.intersects(area) }

    // todo: handle rails/move responsibility to spatial
    override fun getIntersectionPosition(position: Position): Sequence<T> =
        getInTile(position.occupiedTile())
            .filter { position in it.collisionBox }

    override fun getColliding(other: Spatial): Sequence<T> =
        getInArea(other.collisionBox.roundOutToTileBbox())
            .filter { it collidesWith other }

    override fun getPosInCircle(center: Position, radius: Double): Sequence<T> =
        BoundingBox.around(center, radius).roundOutToTileBbox().asSequence()
            .filter { canReachTile(it, center, radius) }
            .flatMap { tile ->
                getInTile(tile).filter {
                    tile == it.position.occupiedTile()
                            && center.squaredDistanceTo(it.position) <= radius * radius
                }
            }

    override fun tileIsOccupied(tile: TilePosition): Boolean = tile in byTile

    override fun occupiedTiles(): Set<TilePosition> = byTile.keys

    private fun canReachTile(
        tile: TilePosition,
        center: Position,
        radius: Double,
    ): Boolean {
        val xDist = min(abs(center.x - tile.x), abs(center.x - (tile.x + 1)))
        val yDist = min(abs(center.y - tile.y), abs(center.y - (tile.y + 1)))
        return xDist * xDist + yDist * yDist <= radius * radius
    }

}

public class WrappingTileHashMap<T : Spatial>(
    public val wrappingSize: TilePosition,
) : TileHashMap<T>() {
    private fun TilePosition.wrap(): TilePosition =
        TilePosition(x.mod(wrappingSize.x), y.mod(wrappingSize.y))

    private fun TileBoundingBox.wrap(): TileBoundingBox {
        val coversX = width >= wrappingSize.x
        val coversY = height >= wrappingSize.y
        return if (coversX) {
            if (coversY) TileBoundingBox(TilePosition.ZERO, wrappingSize)
            else TileBoundingBox(0, minY, wrappingSize.x, maxYExclusive)
        } else {
            if (coversY) TileBoundingBox(minX, 0, maxXExclusive, wrappingSize.y)
            else this
        }
    }

    private fun Position.wrap(): Position =
        Position(x.mod(wrappingSize.x.toDouble()), y.mod(wrappingSize.y.toDouble()))

    override fun addInTileMap(element: T) {
        val tileBox = element.collisionBox.roundOutToTileBbox().wrap()
        for (tile in tileBox) byTile.getOrPut(tile.wrap(), ::hashSetOf).add(element)
    }

    override fun removeInTileMap(element: T) {
        val tileBox = element.collisionBox.roundOutToTileBbox().wrap()
        for (tile in tileBox) {
            byTile[tile.wrap()]?.let {
                it.remove(element)
                if (it.isEmpty()) byTile.remove(tile.wrap())
            }
        }
    }

    override fun getInTile(tile: TilePosition): Sequence<T> = byTile[tile.wrap()]?.asSequence().orEmpty()

    override fun getInArea(area: BoundingBox): Sequence<T> {
        TODO()
    }

    override fun getInArea(area: TileBoundingBox): Sequence<T> =
        area.wrap().asSequence().flatMap { getInTile(it) }.distinct()

    // todo
    // override fun getAtPoint(position: Position): Sequence<T>

    // todo
    // override fun getColliding(other: Spatial): Sequence<T> {

    override fun getPosInCircle(center: Position, radius: Double): Sequence<T> {
        val centerW = center.wrap()
        return BoundingBox.around(centerW, radius)
            .roundOutToTileBbox().wrap()
            .asSequence()
            .filter { canReachTileWrapped(it.wrap(), centerW, radius, wrappingSize) }
            .flatMap { tile ->
                getInTile(tile).filter {
                    tile == it.position.occupiedTile().wrap()
                            && center.squaredDistanceTo(it.position) <= radius * radius
                }
            }
    }

}
