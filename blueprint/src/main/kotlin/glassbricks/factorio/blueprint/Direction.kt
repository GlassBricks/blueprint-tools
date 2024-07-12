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
}
