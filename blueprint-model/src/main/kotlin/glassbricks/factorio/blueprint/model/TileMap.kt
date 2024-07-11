package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.json.Tile
import glassbricks.factorio.blueprint.TilePosition

/**
 * A map from blueprint positions to tile names.
 *
 * A key not present means the tile is empty.
 */
public typealias TileMap = MutableMap<TilePosition, String>

public fun List<Tile>?.toTileMap(): TileMap {
    if(this == null) return HashMap()
    val tileMap = HashMap<TilePosition, String>(size)
    for (tile in this) tileMap[tile.position] = tile.name
    return tileMap
}

public fun TileMap.toTileList(): List<Tile>? {
    if (isEmpty()) return null
    return entries.sortedWith(compareBy({ it.key.x }, { it.key.y })).map { Tile(name = it.value, position = it.key) }
}
