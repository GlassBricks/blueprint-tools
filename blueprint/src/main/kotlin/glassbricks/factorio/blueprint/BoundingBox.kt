package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.Direction.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a simple bounding box (axis aligned).
 */
@Serializable
public data class BoundingBox(
    @SerialName("left_top")
    public val leftTop: Position,
    @SerialName("right_bottom")
    public val rightBottom: Position
) {
    public constructor(minX: Double, minY: Double, maxX: Double, maxY: Double) :
            this(Position(minX, minY), Position(maxX, maxY))

    public val minX: Double get() = leftTop.x
    public val minY: Double get() = leftTop.y
    public val maxX: Double get() = rightBottom.x
    public val maxY: Double get() = rightBottom.y

    public val width: Double get() = maxX - minX
    public val height: Double get() = maxY - minY

    public operator fun contains(point: Position): Boolean = point.x in minX..maxX && point.y in minY..maxY

    public fun translate(amount: Position): BoundingBox = BoundingBox(leftTop + amount, rightBottom + amount)

    /**
     * If the given direction is not a cardinal direction, will snap to the next lowest cardinal direction.
     *
     * North is up; +y is down, +x is right.
     */
    public fun rotateCardinal(direction: Direction): BoundingBox = when (direction) {
        North, Northeast -> this
        East, Southeast -> BoundingBox(-maxY, minX, -minY, maxX)
        South, Southwest -> BoundingBox(-maxX, -maxY, -minX, -minY)
        West, Northwest -> BoundingBox(minY, -maxX, maxY, -minX)
    }

    public fun isEmpty(): Boolean = leftTop == rightBottom

    override fun toString(): String = "BoundingBox(pos($minX, $minY), pos($maxX, $maxY))"

    public companion object {
        public fun around(
            point: Position,
            radius: Double
        ): BoundingBox = bbox(point.x - radius, point.y - radius, point.x + radius, point.y + radius)

        public val EMPTY: BoundingBox = BoundingBox(Position.ZERO, Position.ZERO)
    }
}

public fun bbox(
    minX: Double,
    minY: Double,
    maxX: Double,
    maxY: Double
): BoundingBox = BoundingBox(minX, minY, maxX, maxY)

public fun bbox(leftTop: Position, rightBottom: Position): BoundingBox = BoundingBox(leftTop, rightBottom)
