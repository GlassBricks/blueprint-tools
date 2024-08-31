package glassbricks.factorio.blueprint.placement.grid

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.enumMapOf
import glassbricks.factorio.blueprint.placement.grid.CardinalDirection.*


enum class CardinalDirection {
    North,
    East,
    South,
    West;

    fun oppositeDir(): CardinalDirection = when (this) {
        North -> South
        East -> West
        South -> North
        West -> East
    }

    val axis
        get() = when (this) {
            North, South -> Axis.NorthSouth
            else -> Axis.EastWest
        }
}

@JvmInline
value class Axis private constructor(val isNorthSouth: Boolean) {
    val isEastWest: Boolean get() = !isNorthSouth

    override fun toString(): String = if (isNorthSouth) "NorthSouth" else "EastWest"

    companion object {
        val NorthSouth get() = Axis(true)
        val EastWest get() = Axis(false)
        val values = arrayOf(NorthSouth, EastWest)
    }
}

fun TilePosition.shifted(direction: CardinalDirection, amt: Int = 1): TilePosition = when (direction) {
    North -> add(0, -amt)
    East -> add(amt, 0)
    South -> add(0, amt)
    West -> add(-amt, 0)
}

inline fun <T> mapPerDirection(init: (CardinalDirection) -> T): Map<CardinalDirection, T> {
    val result = enumMapOf<CardinalDirection, T>()
    for (dir in CardinalDirection.entries) {
        result[dir] = init(dir)
    }
    return result
}
