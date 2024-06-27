package glassbricks.factorio.blueprint.json

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Serializable
public data class Position(
    public val x: DoubleAsInt,
    public val y: DoubleAsInt
) {
    public constructor(x: Int, y: Int) : this(x.toDouble(), y.toDouble())
    public operator fun plus(other: Position): Position = Position(x + other.x, y + other.y)
    public operator fun minus(other: Position): Position = Position(x - other.x, y - other.y)

    public operator fun times(scale: Double): Position = Position(x * scale, y * scale)
    public operator fun div(scale: Double): Position = Position(x / scale, y / scale)
    public operator fun times(scale: Int): Position = Position(x * scale, y * scale)
    public operator fun div(scale: Int): Position = Position(x / scale, y / scale)

    public operator fun unaryMinus(): Position = Position(-x, -y)
    public operator fun unaryPlus(): Position = this

    public fun squaredLength(): Double = x * x + y * y
    public fun length(): Double = sqrt(squaredLength())

    public fun squaredDistanceTo(other: Position): Double = (this - other).squaredLength()
    public fun distanceTo(other: Position): Double = (this - other).length()
    public companion object {
        public val ZERO: Position = Position(0.0, 0.0)
    }
}

public operator fun Double.times(position: Position): Position = Position(this * position.x, this * position.y)
public operator fun Int.times(position: Position): Position = Position(this * position.x, this * position.y)

/** Like [Position], but only uses integers. */
@Serializable
public data class TilePosition(val x: Int, val y: Int) {
    public operator fun plus(other: TilePosition): TilePosition = TilePosition(x + other.x, y + other.y)
    public operator fun minus(other: TilePosition): TilePosition = TilePosition(x - other.x, y - other.y)
    
    public operator fun times(scale: Int): TilePosition = TilePosition(x * scale, y * scale)
    public operator fun div(scale: Int): TilePosition = TilePosition(x / scale, y / scale)
    
    public operator fun unaryMinus(): TilePosition = TilePosition(-x, -y)
    public operator fun unaryPlus(): TilePosition = TilePosition(x, y)
    
    public fun squaredLength(): Int = x * x + y * y
    public fun length(): Double = sqrt(squaredLength().toDouble())
    
    public fun center(): Position = Position(x + 0.5, y + 0.5)
    public fun topLeftCorner(): Position = Position(x.toDouble(), y.toDouble())
    
    public fun isZero(): Boolean = x == 0 && y == 0

    public companion object {
        public val ZERO: TilePosition = TilePosition(0, 0)
    }
}

public operator fun Int.times(position: TilePosition): TilePosition = TilePosition(this * position.x, this * position.y)
public fun Position.roundToTilePosition(): TilePosition = TilePosition(x.roundToInt(), y.roundToInt())
