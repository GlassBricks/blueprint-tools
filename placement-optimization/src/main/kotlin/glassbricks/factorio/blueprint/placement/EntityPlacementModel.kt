package glassbricks.factorio.blueprint.placement

import com.google.ortools.Loader
import com.google.ortools.sat.*
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.entity.SpatialDataStructure
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition


public class EntityPlacementModel {
    public val cpModel: CpModel = CpModel()
    public val solver: CpSolver = CpSolver()
    private val _placements: MutableSpatialDataStructure<EntityPlacement<*>> = DefaultSpatialDataStructure()
    public val placements: SpatialDataStructure<EntityPlacement<*>> get() = _placements

    public fun placementIsPossible(placementInfo: Entity<*>): Boolean =
        placements.getColliding(placementInfo).none { it is FixedEntity }

    public fun <P : EntityPrototype> addFixedEntity(entity: Entity<P>): EntityPlacement<P> =
        addFixedEntity(entity.prototype, entity.position, entity.direction)

    public fun <P : EntityPrototype> addFixedEntity(
        prototype: P,
        position: Position,
        direction: Direction = Direction.North
    ): EntityPlacement<P> = FixedEntity(cpModel, prototype, position, direction)
        .also { _placements.add(it) }

    public fun <P : EntityPrototype> addPlacement(entity: Entity<P>, cost: Double = 1.0): EntityPlacementOption<P> =
        addPlacement(
            entity.prototype,
            entity.position,
            entity.direction,
            cost
        )

    public fun <P : EntityPrototype> addPlacement(
        prototype: P, position: Position, direction: Direction
        = Direction.North, cost: Double = 1.0
    ): EntityPlacementOption<P> =
        EntityOptionImpl(
            prototype,
            position,
            direction,
            cost,
            cpModel
        )
            .also { _placements.add(it) }

    public fun <P : EntityPrototype> addFixedEntities(entities: Iterable<Entity<P>>): List<EntityPlacement<P>> =
        entities.map { addFixedEntity(it) }


    public fun removeAll(toRemove: Iterable<EntityPlacement<*>>) {
        for (placement in toRemove) {
            remove(placement)
        }
    }

    public fun remove(placement: EntityPlacement<*>): Boolean {
        val remove = _placements.remove(placement)
        if (!remove) return false
        if (placement is EntityOptionImpl<*> && placement.selectedOrNull != null) {
            error("Removed placement with initialized selected variable")
        }
        return true
    }

    private fun setObjective() {
        val entities = placements
            .filterIsInstance<EntityPlacementOption<*>>()
        val vars = entities
            .map { it.selected }
            .toTypedArray<Literal>()
        val costs = entities.map { it.cost }.toDoubleArray()
        cpModel.minimize(DoubleLinearExpr(vars, costs, 0.0))
    }

    /**
     * This makes the following simplifying assumptions:
     * - Only one entity can occupy a tile; if an entity occupies a tile, it occupies _all_ of the tile
     * - placeable-off-grid is ignored
     * - Collision masks are ignored (everything collides with everything)
     */
    public fun addNonOverlappingConstraints() {
        for (tile in placements.occupiedTiles()) {
            val entities = placements.getInTile(tile).toList()
            if (entities.any { it.isFixed }) {
                for (entity in entities) {
                    if (!entity.isFixed)
                        cpModel.addEquality(entity.selected, 0)
                }
            } else {
                cpModel.addAtMostOne(entities.map { it.selected })
            }
        }
    }

    public var timeLimitInSeconds: Double
        get() = solver.parameters.maxTimeInSeconds
        set(value) {
            solver.parameters.maxTimeInSeconds = value
        }


    public fun solve(
        display: Boolean = true,
        useDefaults: Boolean = true,
    ): PlacementSolution {
        if (useDefaults) {
            addNonOverlappingConstraints()
            setObjective()
        }
        if (display) {
            val toDisplay = listOf(
                "Time",
                "Obj. value",
                "Best bound",
            )
            println(toDisplay.joinToString("\t| ") { it.padEnd(11) })
        }
        val callback = object : CpSolverSolutionCallback() {
            override fun onSolutionCallback() {
                if (display) {
                    println(
                        doubleArrayOf(
                            wallTime(),
                            objectiveValue(),
                            bestObjectiveBound(),
                        ).joinToString("\t| ") {
                            "%.4f".format(it).padEnd(11) }
                    )
                }
            }
        }
        System.gc()
        val status = solver.solve(cpModel, callback)
        return PlacementSolution(
            model = this,
            status = status,
            solver = solver
        )
    }


    public companion object {
        init {
            Loader.loadNativeLibraries()
        }
    }
}

public class PlacementSolution(
    public val model: EntityPlacementModel,
    public val status: CpSolverStatus,
    public val solver: CpSolver,
) {
    public operator fun contains(placement: EntityPlacement<*>): Boolean =
        solver.booleanValue(placement.selected)

    @Suppress("UNCHECKED_CAST")
    public fun getSelectedOptionalEntities(): List<EntityPlacementOption<*>> =
        model.placements.filter {
            it is EntityPlacementOption<*> &&
                    !it.isFixed && solver.booleanValue(it.selected)
        } as List<EntityPlacementOption<*>>

    public fun getSelectedEntities(): List<EntityPlacement<*>> =
        model.placements.filter { solver.booleanValue(it.selected) }
}

public fun <P : EntityPrototype> EntityPlacementModel.getAllPossibleUnrotatedPlacements(
    prototypes: Iterable<P>,
    bounds: TileBoundingBox,
): List<BasicEntity<P>> {
    val boundsDouble = bounds.toBoundingBox()
    val tileBounds = bounds.iterateTiles().toList()
    return prototypes.toSet().parallelStream().flatMap { prototype: P ->
        tileBounds.parallelStream().map { tile ->
            BasicEntity(
                prototype,
                prototype.tileSnappedPosition(tile),
            )
        }.filter {
            it.collisionBox in boundsDouble && this.placementIsPossible(it)
        }
    }.toList()
}
