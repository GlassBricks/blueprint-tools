package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.TilePosition


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

fun Direction.toCardinalDirection(): CardinalDirection? = when (this) {
    Direction.North -> CardinalDirection.North
    Direction.East -> CardinalDirection.East
    Direction.South -> CardinalDirection.South
    Direction.West -> CardinalDirection.West
    else -> null
}

fun CardinalDirection.toFactorioDirection(): Direction = when (this) {
    CardinalDirection.North -> Direction.North
    CardinalDirection.East -> Direction.East
    CardinalDirection.South -> Direction.South
    CardinalDirection.West -> Direction.West
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
    CardinalDirection.North -> add(0, -amt)
    CardinalDirection.East -> add(amt, 0)
    CardinalDirection.South -> add(0, amt)
    CardinalDirection.West -> add(-amt, 0)
}
