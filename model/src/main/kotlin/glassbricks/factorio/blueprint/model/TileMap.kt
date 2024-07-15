package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.json.Tile

/**
 * A map from blueprint positions to tile names.
 *
 * A key not present means the tile is empty.
 */
public typealias TileMap = MutableMap<TilePosition, String>

public fun List<Tile>?.toTileMap(): TileMap {
    if (this == null) return HashMap()
    val tileMap = HashMap<TilePosition, String>(size)
    for (tile in this) tileMap[tile.position] = tile.name
    return tileMap
}

public fun TileMap.toTileList(): List<Tile>? {
    if (isEmpty()) return null
    return entries.sortedBy { it.key }.map { Tile(position = it.key, name = it.value) }
}
