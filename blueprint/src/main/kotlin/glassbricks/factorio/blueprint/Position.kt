package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.json.DoubleAsInt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Stored as 32 bit numbers, with 8 bits for the fractional part.
 *
 * [x] and [y] hold the values as doubles, you can normally use those.
 */
@Suppress("DataClassPrivateConstructor")
@Serializable(with = Position.Serializer::class)
public data class Position private constructor(
    public val xAsInt: Int,
    public val yAsInt: Int,
) : Comparable<Position> {
    public constructor(x: Double, y: Double) : this((x * 256).roundToInt(), (y * 256).roundToInt())

    val x: Double get() = xAsInt / 256.0
    val y: Double get() = yAsInt / 256.0

    public operator fun plus(other: Position): Position = Position(xAsInt + other.xAsInt, yAsInt + other.yAsInt)
    public operator fun minus(other: Position): Position = Position(xAsInt - other.xAsInt, yAsInt - other.yAsInt)

    public operator fun times(scale: Double): Position =
        Position((xAsInt * scale).roundToInt(), (yAsInt * scale).roundToInt())

    public operator fun times(scale: Int): Position = Position(xAsInt * scale, yAsInt * scale)
    public operator fun div(scale: Double): Position =
        Position((xAsInt / scale).roundToInt(), (yAsInt / scale).roundToInt())

    public operator fun div(scale: Int): Position = Position(xAsInt / scale, yAsInt / scale)

    public operator fun unaryPlus(): Position = this
    public operator fun unaryMinus(): Position = Position(-xAsInt, -yAsInt)

    public fun squaredLength(): Double = (xAsInt.toLong() * xAsInt + yAsInt.toLong() * yAsInt) / (256.0 * 256.0)
    public fun length(): Double = sqrt(squaredLength())

    public fun squaredDistanceTo(other: Position): Double = (this - other).squaredLength()
    public fun distanceTo(other: Position): Double = (this - other).length()

    /**
     * Returns the tile position of the tile this position is in.
     */
    public fun occupiedTile(): TilePosition = TilePosition(floor(x).toInt(), floor(y).toInt())


    override fun toString(): String = "Position($x, $y)"

    override fun compareTo(other: Position): Int {
        val xComp = xAsInt.compareTo(other.xAsInt)
        return if (xComp != 0) xComp else yAsInt.compareTo(other.yAsInt)
    }


    public companion object {
        public val ZERO: Position = Position(0, 0)
    }

    @Serializable
    private class DoublePosition(val x: DoubleAsInt, val y: DoubleAsInt)

    internal object Serializer : KSerializer<Position> {
        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            SerialDescriptor(Position::class.qualifiedName!!, DoublePosition.serializer().descriptor)

        override fun deserialize(decoder: Decoder): Position =
            decoder.decodeSerializableValue(DoublePosition.serializer())
                .let { Position(it.x, it.y) }

        override fun serialize(encoder: Encoder, value: Position) {
            encoder.encodeSerializableValue(DoublePosition.serializer(), DoublePosition(value.x, value.y))
        }
    }
}

public operator fun Double.times(position: Position): Position = position * this
public operator fun Int.times(position: Position): Position = position * this

/** Short for [Position] constructor. */
public fun pos(x: Double, y: Double): Position = Position(x, y)

/**
 * Like [Position], but represents only tiles as integers.
 *
 * The top left corner of a tile is the tile's position as a MapPosition.
 */
@Serializable
public data class TilePosition(val x: Int, val y: Int) : Comparable<TilePosition> {
    public operator fun plus(other: TilePosition): TilePosition = TilePosition(x + other.x, y + other.y)
    public operator fun minus(other: TilePosition): TilePosition = TilePosition(x - other.x, y - other.y)

    public operator fun times(scale: Int): TilePosition = TilePosition(x * scale, y * scale)

    public operator fun unaryMinus(): TilePosition = TilePosition(-x, -y)
    public operator fun unaryPlus(): TilePosition = TilePosition(x, y)

    public fun squaredLength(): Int = x * x + y * y
    public fun length(): Double = sqrt(squaredLength().toDouble())

    public fun center(): Position = Position(x + 0.5, y + 0.5)
    public fun topLeftCorner(): Position = Position(x.toDouble(), y.toDouble())

    public fun isZero(): Boolean = x == 0 && y == 0


    /** Gets the map position bounding box of this tile. */
    public fun mapBoundingBox(): BoundingBox = BoundingBox(pos(x.toDouble(), y.toDouble()), pos(x + 1.0, y + 1.0))

    override fun compareTo(other: TilePosition): Int {
        val xComp = x.compareTo(other.x)
        return if (xComp != 0) xComp else y.compareTo(other.y)
    }

    public companion object {
        public val ZERO: TilePosition = TilePosition(0, 0)
    }
}

public operator fun Int.times(position: TilePosition): TilePosition = position * this

public fun tilePos(x: Int, y: Int): TilePosition = TilePosition(x, y)
