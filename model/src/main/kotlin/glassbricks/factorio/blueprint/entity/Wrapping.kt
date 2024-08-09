package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.TilePosition
import kotlin.math.abs
import kotlin.math.min


public fun vector1dWrapped(
    from: Double,
    to: Double,
    wrapLen: Int
): Double {
    val va = to - from
    val vb = if (va > 0) va - wrapLen else va + wrapLen
    return if (abs(va) < abs(vb)) va else vb
}

public fun distanceWrapped(
    a: Double,
    b: Double,
    wrapLen: Int
): Double {
    if (b > a) {
        return min(b - a, a + wrapLen - b)
    }
    return min(a - b, b + wrapLen - a)
}

public fun vectorWrapped(
    from: Position,
    to: Position,
    wrapSize: TilePosition
): Position {
    val x = vector1dWrapped(from.x, to.x, wrapSize.x)
    val y = vector1dWrapped(from.y, to.y, wrapSize.y)
    return Position(x, y)
}

public fun distanceSquaredWrapped(
    a: Position,
    b: Position,
    wrapSize: TilePosition
): Double {
    val xDist = distanceWrapped(a.x, b.x, wrapSize.x)
    val yDist = distanceWrapped(a.y, b.y, wrapSize.y)
    return xDist * xDist + yDist * yDist
}


/**
 * Assumes tile and position are already wrapped.
 */
public fun canReachTileWrapped(
    tile: TilePosition,
    position: Position,
    distance: Double,
    wrappingSize: TilePosition
): Boolean {
    val xDist = minOf(
        distanceWrapped(position.x, tile.x.toDouble(), wrappingSize.x),
        distanceWrapped(position.x, tile.x + 1.0, wrappingSize.x)
    )
    val yDist = minOf(
        distanceWrapped(position.y, tile.y.toDouble(), wrappingSize.y),
        distanceWrapped(position.y, tile.y + 1.0, wrappingSize.y)
    )
    return xDist * xDist + yDist * yDist <= distance * distance
}
