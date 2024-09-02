package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.RailCollisionBoxes
import glassbricks.factorio.blueprint.RotatedRectangle
import glassbricks.factorio.blueprint.Spatial
import glassbricks.factorio.blueprint.getEnclosingBox
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.CurvedRailPrototype
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.RailPrototype


public sealed class Rail(json: EntityJson) : BaseEntity(json) {
    abstract override val prototype: RailPrototype
    override fun exportToJson(json: EntityJson) {
        // nothing to do
    }

    // todo: cleanup needing to use getter stuff

    public fun getAABBBox(): BoundingBox? {
        if (this.isSimpleCollisionBox) return this.collisionBox
        this.collisionBox // call getter
        return primaryCollisionBox
    }

    public fun getRectangleBox(): RotatedRectangle? {
        this.collisionBox // call getter
        return secondaryCollisionBox
    }

    @JvmField
    protected var primaryCollisionBox: BoundingBox? = null

    @JvmField
    protected var secondaryCollisionBox: RotatedRectangle? = null

    abstract override val isSimpleCollisionBox: Boolean
    abstract override fun computeThisCollisionBox(
        prototype: EntityPrototype,
        position: Position,
        direction: Direction,
    ): BoundingBox

    override fun intersects(area: BoundingBox): Boolean {
        if (this.isSimpleCollisionBox) return super.intersects(area)
        this.collisionBox // call getter to set
        return primaryCollisionBox?.intersects(area) == true || secondaryCollisionBox!!.intersects(area)
    }

    override fun collidesWith(other: Spatial): Boolean {
        if (this.isSimpleCollisionBox) return super.collidesWith(other)
        if (!(collisionMask collidesWith other.collisionMask)) return false
        if (!other.isSimpleCollisionBox) throw NotImplementedError("Intersecting multiple rotated boxes")
        return this.intersects(other.collisionBox)
    }


    abstract override fun copyIsolated(): Rail
}

public class StraightRail(
    override val prototype: RailPrototype,
    json: EntityJson,
) : Rail(json) {
    override val isSimpleCollisionBox: Boolean get() = direction.isCardinal

    override fun computeThisCollisionBox(
        prototype: EntityPrototype,
        position: Position,
        direction: Direction,
    ): BoundingBox {
        primaryCollisionBox = null
        secondaryCollisionBox = null
        return if (isSimpleCollisionBox) {
            RailCollisionBoxes.straightPrimary
                .rotateCardinal(direction)
                .translate(position)
        } else {
            val directionRoundDown = Direction.entries[(direction.ordinal / 2) * 2]
            val secondaryBox = RailCollisionBoxes.diagonalPrimary
                .getEntityCollisionBox(directionRoundDown, position)
            secondaryCollisionBox = secondaryBox
            secondaryBox.broadCollisionBox
        }
    }

    override fun copyIsolated(): StraightRail = StraightRail(prototype, jsonForCopy())
}

public class CurvedRail(
    override val prototype: CurvedRailPrototype,
    json: EntityJson,
) : Rail(json) {
    override val isSimpleCollisionBox: Boolean get() = false
    override fun computeThisCollisionBox(
        prototype: EntityPrototype,
        position: Position,
        direction: Direction,
    ): BoundingBox {
        val isLeft = direction.isCardinal
        val basePrimaryBox =
            if (isLeft) RailCollisionBoxes.curvedRailPrimaryLeft else RailCollisionBoxes.curvedRailPrimaryRight
        this.primaryCollisionBox = basePrimaryBox.rotateCardinal(direction).translate(position)
        val baseSecondaryBox = if (isLeft)
            RailCollisionBoxes.curvedRailSecondaryLeft else RailCollisionBoxes.curvedRailSecondaryRight
        val roundDown = Direction.entries[(direction.ordinal / 2) * 2]
        this.secondaryCollisionBox = baseSecondaryBox.getEntityCollisionBox(roundDown, position)

        return getEnclosingBox(listOf(primaryCollisionBox!!, secondaryCollisionBox!!.broadCollisionBox))
    }

    override fun copyIsolated(): CurvedRail = CurvedRail(prototype, jsonForCopy())
}
