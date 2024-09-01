package glassbricks.factorio.blueprint.placement

import com.google.ortools.sat.CpModel
import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.basicEntityJson
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.prototypes.EntityPrototype

/**
 * An option for placement, in optimization.
 *
 * @property cost The cost of using this option.
 */
sealed interface EntityPlacement<out P : EntityPrototype> : Entity<P> {
    val selected: Literal

    /** If this placement is forced to be used. */
    val isFixed: Boolean

    /** This may be identity identical to the passed in entity, if it was a BlueprintEntity */
    fun toBlueprintEntity(): BlueprintEntity
}

sealed interface OptionalEntityPlacement<out P : EntityPrototype> : EntityPlacement<P> {
    /** The cost of using this option. */
    var cost: Double
    override val isFixed: Boolean get() = false
}

fun EntityPlacement<*>.toEntity(): BlueprintEntity =
    createBpEntity(prototype, basicEntityJson(prototype.name, position, direction))

internal class FixedEntity<out P : EntityPrototype>(
    cp: CpModel,
    val entity: Entity<P>,
) : EntityPlacement<P>, Entity<P> by entity {
    override val selected: Literal = cp.trueLiteral()
    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)
    override val isFixed: Boolean get() = true

    override fun toBlueprintEntity(): BlueprintEntity =
        (entity as? BlueprintEntity) ?: createBpEntity(prototype, position, direction)

    override fun toString(): String = "FixedEntity(prototype=$prototype, position=$position, direction=$direction)"
}

internal class OptionalEntity<out P : EntityPrototype>(
    val entity: Entity<P>,
    override var cost: Double,
    selected: Literal?,
    private val model: CpModel,
) : OptionalEntityPlacement<P>, Entity<P> by entity {
    override fun toBlueprintEntity(): BlueprintEntity =
        (entity as? BlueprintEntity) ?: createBpEntity(prototype, position, direction)

    internal var selectedOrNull: Literal? = selected
        private set

    override val selected: Literal
        get() = selectedOrNull ?: synchronized(model) {
            selectedOrNull ?: (model.newBoolVar("selected_${prototype.name}_${position.x}_${position.y}_${direction}"))
                .also { selectedOrNull = it }
        }

    override fun toString(): String = "EntityOptionImpl(prototype=$prototype, position=$position, direction=$direction)"
}
