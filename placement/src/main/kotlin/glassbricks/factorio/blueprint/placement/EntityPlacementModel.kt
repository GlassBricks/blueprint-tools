package glassbricks.factorio.blueprint.placement

import com.google.ortools.Loader
import com.google.ortools.sat.*
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.entity.SpatialDataStructure
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition


class EntityPlacementModel(
    private val _placements: MutableSpatialDataStructure<EntityPlacement<*>> = DefaultSpatialDataStructure(),
    override val cp: CpModel = CpModel(),
) : WithCp {
    val solver: CpSolver = CpSolver()
    val placements: SpatialDataStructure<EntityPlacement<*>> get() = _placements

    fun canPlace(placementInfo: Entity<*>): Boolean =
        placements.getColliding(placementInfo).none { it is FixedEntity }

    fun <P : EntityPrototype> addFixedPlacement(entity: Entity<P>): EntityPlacement<P> {
        return FixedEntity(cp, entity)
            .also { _placements.add(it) }
    }

    fun <P : EntityPrototype> addFixedPlacement(
        prototype: P,
        position: Position,
        direction: Direction = Direction.North,
    ): EntityPlacement<P> = addFixedPlacement(BasicEntity(prototype, position, direction))

    fun <P : EntityPrototype> addFixedEntities(entities: Iterable<Entity<P>>): List<EntityPlacement<P>> =
        entities.map { addFixedPlacement(it) }

    fun <P : EntityPrototype> addPlacement(
        entity: Entity<P>,
        cost: Double = 1.0,
        selectedLiteral: Literal? = null,
    ): OptionalEntityPlacement<P> =
        OptionalEntity(
            entity,
            cost,
            selectedLiteral,
            cp
        ).also { _placements.add(it) }

    fun <P : EntityPrototype> addPlacement(
        prototype: P,
        position: Position,
        direction: Direction = Direction.North,
        cost: Double = 1.0,
        selectedLiteral: Literal? = null,
    ): OptionalEntityPlacement<P> =
        addPlacement(BasicEntity(prototype, position, direction), cost, selectedLiteral)

    /**
     * Slightly optimized [addPlacement] that skips if a known fixed entity is in the way.
     *
     * Only here to save a bit of memory.
     */
    fun <P : EntityPrototype> addPlacementIfPossible(
        entity: Entity<P>,
        cost: Double = 1.0,
    ): OptionalEntityPlacement<P>? {
        if (!canPlace(entity)) return null
        return addPlacement(entity, cost)
    }

    fun <P : EntityPrototype> addPlacementIfPossible(
        prototype: P,
        position: Position,
        direction: Direction = Direction.North,
        cost: Double = 1.0,
    ): OptionalEntityPlacement<P>? = addPlacementIfPossible(BasicEntity(prototype, position, direction), cost)

    fun remove(placement: EntityPlacement<*>): Boolean {
        val remove = _placements.remove(placement)
        if (!remove) return false
        if (placement is OptionalEntity<*> && placement.selectedOrNull != null) {
            error("Removed placement with initialized selected variable")
        }
        return true
    }

    fun removeAll(toRemove: Iterable<EntityPlacement<*>>) {
        for (placement in toRemove) {
            remove(placement)
        }
    }


    private fun setObjective() {
        val entities = placements
            .filterIsInstance<OptionalEntityPlacement<*>>()
        val vars = entities.map { it.selected }.toTypedArray()
        val costs = entities.map { it.cost }.toDoubleArray()
        cp.minimize(DoubleLinearExpr(vars, costs, 0.0))
    }

    /**
     * This makes the following simplifying assumptions:
     * - Only one entity can occupy a tile; if an entity occupies a tile, it occupies _all_ of the tile
     * - placeable-off-grid is ignored
     * - Collision masks are ignored (everything collides with everything)
     * - If fixed entities overlap, we still allow it (handles some edge cases due to assumptions from above)
     */
    private fun addNonOverlappingConstraint() {
        for (tile in placements.occupiedTiles()) {
            val entities = placements.getInTile(tile).toList()
            if (entities.any { it.isFixed }) {
                for (entity in entities) {
                    if (!entity.isFixed)
                        cp.addEquality(entity.selected, 0)
                }
            } else {
                cp.addAtMostOne(entities.map { it.selected })
            }
        }
    }

    var timeLimitInSeconds: Double
        get() = solver.parameters.maxTimeInSeconds
        set(value) {
            solver.parameters.maxTimeInSeconds = value
        }


    fun solve(
        display: Boolean = true,
    ): PlacementSolution {
        addNonOverlappingConstraint()
        setObjective()

        if (display) {
            val toDisplay = listOf(
                "Time",
                "Obj. value",
                "Best bound",
            )
            println(toDisplay.joinToString("\t| ") { it.padEnd(11) })
        }
        val callback = if (!display) null else object : CpSolverSolutionCallback() {
            override fun onSolutionCallback() {
                println(
                    doubleArrayOf(
                        wallTime(),
                        objectiveValue(),
                        bestObjectiveBound(),
                    ).joinToString("\t| ") {
                        "%.4f".format(it).padEnd(11)
                    }
                )
            }
        }
        System.gc()
        val status = solver.solve(cp, callback)
        return PlacementSolution(
            model = this,
            status = status,
            solver = solver
        )
    }


    companion object {
        init {
            Loader.loadNativeLibraries()
        }
    }
}

class PlacementSolution(
    val model: EntityPlacementModel,
    val status: CpSolverStatus,
    val solver: CpSolver,
) {
    val isOk: Boolean get() = status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE

    operator fun contains(placement: EntityPlacement<*>): Boolean =
        solver.booleanValue(placement.selected)

    @Suppress("UNCHECKED_CAST")
    fun getSelectedOptionalEntities(): List<OptionalEntityPlacement<*>> =
        model.placements.filter {
            it is OptionalEntityPlacement<*> &&
                    !it.isFixed && solver.booleanValue(it.selected)
        } as List<OptionalEntityPlacement<*>>

    fun getSelectedEntities(): List<EntityPlacement<*>> =
        model.placements.filter {
            solver.booleanValue(it.selected)
        }
}


fun PlacementSolution.toBlueprintEntities(): MutableSpatialDataStructure<BlueprintEntity> {
    val result = DefaultSpatialDataStructure<BlueprintEntity>()
    for (entity in getSelectedEntities()) {
        result.add(entity.toBlueprintEntity())
    }
    return result
}

fun <P : EntityPrototype> EntityPlacementModel.getAllPossibleUnrotatedPlacements(
    prototypes: Iterable<P>,
    bounds: TileBoundingBox,
): List<BasicEntity<P>> {
    val boundsDouble = bounds.toBoundingBox()
    val tileBounds = bounds.toList()
    return prototypes.toSet().parallelStream().flatMap { prototype: P ->
        tileBounds.parallelStream().map { tile ->
            BasicEntity(
                prototype,
                prototype.tileSnappedPosition(tile),
            )
        }.filter {
            it.collisionBox in boundsDouble && this.canPlace(it)
        }
    }.toList()
}
