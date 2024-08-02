package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.json.EnumOrdinalSerializer
import kotlinx.serialization.Serializable

/**
 * The [ordinal] represents the encoded direction.
 */
@Serializable(with = Direction.Serializer::class)
public enum class Direction {
    North,
    Northeast,
    East,
    Southeast,
    South,
    Southwest,
    West,
    Northwest;

    public companion object {
        public fun fromByte(byte: Byte): Direction = entries[byte.toInt()]
    }

    public fun toByte(): Byte = ordinal.toByte()

    public fun oppositeDirection(): Direction = entries[(ordinal + 4) % 8]
    public val isCardinal: Boolean get() = ordinal % 2 == 0

    internal object Serializer : EnumOrdinalSerializer<Direction>(Direction::class)

    public fun toTilePosVector(): TilePosition = when (this) {
        North -> tilePos(0, -1)
        Northeast -> tilePos(1, -1)
        East -> tilePos(1, 0)
        Southeast -> tilePos(1, 1)
        South -> tilePos(0, 1)
        Southwest -> tilePos(-1, 1)
        West -> tilePos(-1, 0)
        Northwest -> tilePos(-1, -1)
    }

    public fun toPosVector(): Position = toTilePosVector().topLeftCorner()
}

public fun TilePosition.shifted(direction: Direction, amt: Int = 1): TilePosition {
    return when (direction) {
        Direction.North -> this + tilePos(0, -amt)
        Direction.Northeast -> this + tilePos(amt, -amt)
        Direction.East -> this + tilePos(amt, 0)
        Direction.Southeast -> this + tilePos(amt, amt)
        Direction.South -> this + tilePos(0, amt)
        Direction.Southwest -> this + tilePos(-amt, amt)
        Direction.West -> this + tilePos(-amt, 0)
        Direction.Northwest -> this + tilePos(-amt, -amt)
    }
}
