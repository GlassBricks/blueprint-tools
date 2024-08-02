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
interface EntityPlacement<out P : EntityPrototype> : Entity<P> {
    val selected: Literal

    /** If this placement is forced to be used. */
    val isFixed: Boolean
}

interface OptionalEntityPlacement<out P : EntityPrototype> : EntityPlacement<P> {
    /** The cost of using this option. */
    var cost: Double
    override val isFixed: Boolean get() = false
}


fun EntityPlacement<*>.toEntity(): BlueprintEntity =
    createBpEntity(prototype, basicEntityJson(prototype.name, position, direction))

internal class FixedEntity<out P : EntityPrototype>(
    cpModel: CpModel,
    override val prototype: P,
    override var position: Position,
    override val direction: Direction,
) : EntityPlacement<P>, Entity<P> {
    override val selected: Literal = cpModel.trueLiteral()
    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)
    override val isFixed: Boolean get() = true

    override fun toString(): String = "FixedEntity(prototype=$prototype, position=$position, direction=$direction)"
}

internal class EntityOptionImpl<out P : EntityPrototype>(
    override val prototype: P,
    override var position: Position,
    override val direction: Direction,
    override var cost: Double,
    private val model: CpModel,
) : OptionalEntityPlacement<P> {
    private var _selected: Literal? = null
    override val selected: Literal
        get() = _selected ?: synchronized(model) {
            _selected ?: (model.newBoolVar("selected_${prototype.name}_${position.x}_${position.y}_${direction}"))
                .also { _selected = it }
        }
    val selectedOrNull: Literal? get() = _selected

    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)

    override fun toString(): String = "EntityOptionImpl(prototype=$prototype, position=$position, direction=$direction)"
}
