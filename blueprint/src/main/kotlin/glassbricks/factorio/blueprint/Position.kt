package glassbricks.factorio.blueprint

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
public data class Position(
    public val x: DoubleAsInt,
    public val y: DoubleAsInt
) {
    public operator fun plus(other: Position): Position = Position(x + other.x, y + other.y)
    public operator fun minus(other: Position): Position = Position(x - other.x, y - other.y)

    public operator fun times(scale: Double): Position = Position(x * scale, y * scale)
    public operator fun div(scale: Double): Position = Position(x / scale, y / scale)
    public operator fun times(scale: Int): Position = Position(x * scale, y * scale)
    public operator fun div(scale: Int): Position = Position(x / scale, y / scale)

    public operator fun unaryMinus(): Position = Position(-x, -y)
    public operator fun unaryPlus(): Position = Position(x, y)

    public fun squaredLength(): Double = x * x + y * y
    public fun length(): Double = sqrt(squaredLength())

    public fun squaredDistanceTo(other: Position): Double = (this - other).squaredLength()
    public fun distanceTo(other: Position): Double = (this - other).length()
}

public operator fun Double.times(position: Position): Position = Position(this * position.x, this * position.y)
public operator fun Int.times(position: Position): Position = Position(this * position.x, this * position.y)
public operator fun Double.div(position: Position): Position = Position(this / position.x, this / position.y)
public operator fun Int.div(position: Position): Position = Position(this / position.x, this / position.y)
