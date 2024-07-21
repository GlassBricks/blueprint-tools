package glassbricks.factorio.blueprint.placement

import com.google.ortools.sat.CpModel
import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.basicEntityJson
import glassbricks.factorio.blueprint.entity.createEntity
import glassbricks.factorio.blueprint.prototypes.EntityPrototype

/**
 * An option for placement, in optimization.
 *
 * @property cost The cost of using this option.
 */
public interface EntityPlacement<out P : EntityPrototype> : Entity<P> {
    public val selected: Literal

    /**
     * If this placement is forced to be used.
     */
    public val isFixed: Boolean
}

public interface EntityPlacementOption<out P : EntityPrototype> : EntityPlacement<P> {
    /** The cost of using this option. */
    public var cost: Double
    override val isFixed: Boolean get() = false
}


public fun EntityPlacement<*>.toEntity(): BlueprintEntity =
    createEntity(prototype, basicEntityJson(prototype.name, position, direction))

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
) : EntityPlacementOption<P> {
    private var _selected: Literal? = null
    override val selected: Literal
        get() = _selected ?: synchronized(model) {
            _selected ?: (model.newBoolVar("selected_${prototype.name}_${position.x}_${position.y}_${direction}"))
                .also { _selected = it }
        }
    val selectedOrNull: Literal? get() = _selected

    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)

    override fun toString(): String = "EntityOptionImpl(prototype=$prototype, position=$position, direction=$direction)gg"
}
