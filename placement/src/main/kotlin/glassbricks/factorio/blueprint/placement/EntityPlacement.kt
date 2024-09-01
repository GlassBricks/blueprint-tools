package glassbricks.factorio.blueprint.placement

import com.google.ortools.sat.CpModel
import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.SpatialDataStructure
import glassbricks.factorio.blueprint.entity.basicEntityJson
import glassbricks.factorio.blueprint.entity.copyWithOldConnections
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.entity.findMatching
import glassbricks.factorio.blueprint.prototypes.EntityPrototype

/**
 * An option for placement, in optimization.
 */
sealed interface EntityPlacement<out P : EntityPrototype> : Entity<P> {
    val selected: Literal

    val isFixed: Boolean

    val originalEntity: Entity<*>?
}

sealed interface OptionalEntityPlacement<out P : EntityPrototype> : EntityPlacement<P> {
    /** The cost of using this option. */
    var cost: Double

    override val isFixed: Boolean get() = false
}

/** The returned entity won't have any settings beyond basic ones */
private fun EntityPlacement<*>.createNewBpEntity(): BlueprintEntity =
    createBpEntity(prototype, basicEntityJson(prototype.name, position, direction))

fun EntityPlacement<*>.toBlueprintEntity(
    existingToCopyFrom: SpatialDataStructure<BlueprintEntity>? = null,
): BlueprintEntity {
    val existingBpEntity = originalEntity as? BlueprintEntity ?: existingToCopyFrom?.findMatching(this)
    if (existingBpEntity != null) {
        val result = existingBpEntity.copyWithOldConnections()
        check(result.prototype == prototype)
        result.position = position
        result.direction = direction
        return result
    }
    return createNewBpEntity()
}

internal class FixedPlacement<out P : EntityPrototype>(
    cp: CpModel,
    override val originalEntity: Entity<*>?,
    override val prototype: P,
    override val position: Position,
    override val direction: Direction,
) : EntityPlacement<P> {
    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)
    override val isFixed: Boolean get() = true

    override val selected: Literal = cp.trueLiteral()
    override fun toString(): String = "FixedEntity(prototype=$prototype, position=$position, direction=$direction)"
}

internal class OptionalPlacement<out P : EntityPrototype>(
    override val originalEntity: Entity<*>?,
    override val prototype: P,
    override val position: Position,
    override val direction: Direction,
    override var cost: Double,
    override val selected: Literal,
) : OptionalEntityPlacement<P> {
    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)

    override fun toString(): String = "EntityOptionImpl(prototype=$prototype, position=$position, direction=$direction)"
}
