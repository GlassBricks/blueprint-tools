package glassbricks.factorio.blueprint

import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin


/**
 * These found by printing collision box of rail in-game
 */
public object RailCollisionBoxes {
    public val straightPrimary: BoundingBox = BoundingBox(-0.69921875, -0.98828125, 0.69921875, 0.98828125)

    public val diagonalPrimary: RotatedRectangle = run {
        val bbox = BoundingBox(-0.15234375, -1.1953125, 1.14453125, 0.203125)
        RotatedRectangle(
            center = bbox.centerVec(),
            width = bbox.width,
            height = bbox.height,
            angleRadians = toRadians(-45.0)
        )
    }

    public val curvedRailPrimaryLeft: BoundingBox = BoundingBox(
        0.24609375, 1.73828125, 1.74609375, 3.8828125
    )
    public val curvedRailPrimaryRight: BoundingBox = BoundingBox(
        -curvedRailPrimaryLeft.maxX, curvedRailPrimaryLeft.minY, -curvedRailPrimaryLeft.minX, curvedRailPrimaryLeft.maxY
    )
    public val curvedRailSecondaryLeft: RotatedRectangle = run {
        val bbox = BoundingBox(-1.0390625, -3.3984375, 0.3203125, 2.0)
        RotatedRectangle(
            center = bbox.centerVec(),
            width = bbox.width,
            height = bbox.height,
            angleRadians = -toRadians(36.0) // 0.1 of a rotation
        )
    }
    public val curvedRailSecondaryRight: RotatedRectangle = curvedRailSecondaryLeft.copy(
        center = curvedRailSecondaryLeft.center.let { it.copy(x = -it.x) },
        angleRadians = -curvedRailSecondaryLeft.angleRadians
    )
}

public data class RotatedRectangle(
    public val center: Vector,
    public val width: Double,
    public val height: Double,
    public val angleRadians: Double,
) {
    public val points: Array<Position> = run {
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        Array(4) { i ->
            val x = if (i == 0 || i == 3) -width / 2 else width / 2
            val y = if (i and 2 == 0) -height / 2 else height / 2
            val rotatedX = x * cos - y * sin
            val rotatedY = x * sin + y * cos
            Position(center.x + rotatedX, center.y + rotatedY)
        }
    }
    public val broadCollisionBox: BoundingBox
        get() = BoundingBox(
            points.minOf { it.x },
            points.minOf { it.y },
            points.maxOf { it.x },
            points.maxOf { it.y }
        )

    public fun getEntityCollisionBox(
        direction: Direction,
        position: Position,
    ): RotatedRectangle = copy(
        center = center.rotate(direction) + position.toVector(),
        angleRadians = angleRadians + direction.radians
    )

    public operator fun contains(point: Position): Boolean {
        // project point onto x and y axis
        val x = point.x - center.x
        val y = point.y - center.y
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        val rotatedX = x * cos + y * sin
        val rotatedY = -x * sin + y * cos
        return rotatedX in -width / 2..width / 2 && rotatedY in -height / 2..height / 2
    }

    public fun intersects(aabb: BoundingBox): Boolean {
        // if projection of points onto x and y axis are disjoint, then the two boxes are disjoint
        val points = points
        if (points.all { it.x < aabb.minX }
            || points.all { it.x > aabb.maxX }
            || points.all { it.y < aabb.minY }
            || points.all { it.y > aabb.maxY }
        ) return false
        val aabbPoints = listOf(
            aabb.leftTop - center,
            Position(aabb.maxX, aabb.minY) - center,
            Position(aabb.minX, aabb.maxY) - center,
            aabb.rightBottom - center
        )
        // project aabb points onto this rectangle axis
        // x and y rotated by deg
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)

        // Todo: remove allocations?
        val projectXNorm = aabbPoints.map { it.x * -sin + it.y * cos }
        if (projectXNorm.all { it < -height / 2 } || projectXNorm.all { it > height / 2 }) return false

        val projectYNorm = aabbPoints.map { it.x * cos + it.y * sin }
        if (projectYNorm.all { it < -width / 2 } || projectYNorm.all { it > width / 2 }) return false

        return true
    }

}
