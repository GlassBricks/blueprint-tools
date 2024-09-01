package glassbricks.factorio.blueprint.placement

import com.google.ortools.Loader
import com.google.ortools.sat.*
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.entity.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.entity.SpatialDataStructure
import glassbricks.factorio.blueprint.prototypes.EntityPrototype


class EntityPlacementModel(
    private val _placements: MutableSpatialDataStructure<EntityPlacement<*>> = DefaultSpatialDataStructure(),
    override val cp: CpModel = CpModel(),
) : WithCp {
    val solver: CpSolver = CpSolver()
    val placements: SpatialDataStructure<EntityPlacement<*>> get() = _placements

    fun canPlace(placementInfo: Entity<*>): Boolean =
        placements.getColliding(placementInfo).none { it.isFixed }

    fun <P : EntityPrototype> addFixedPlacement(
        prototype: P,
        position: Position,
        direction: Direction = Direction.North,
    ): EntityPlacement<P> {
        val entity = FixedPlacement(
            cp = cp,
            originalEntity = null,
            prototype = prototype,
            position = position,
            direction = direction
        )
        _placements.add(entity)
        return entity
    }

    fun <P : EntityPrototype> addFixedPlacement(
        entity: Entity<P>,
        position: Position = entity.position,
        direction: Direction = entity.direction,
    ): EntityPlacement<P> {
        val fixedEntity = FixedPlacement(
            cp = cp,
            originalEntity = entity,
            prototype = entity.prototype,
            position = position,
            direction = direction
        )
        _placements.add(fixedEntity)
        return fixedEntity
    }

    fun <P : EntityPrototype> addFixedEntities(entities: Iterable<Entity<P>>): List<EntityPlacement<P>> =
        entities.map { addFixedPlacement(it) }

    fun <P : EntityPrototype> addPlacement(
        prototype: P,
        position: Position,
        direction: Direction = Direction.North,
        cost: Double = 1.0,
        selected: Literal = cp.newBoolVar("selected_${prototype.name}_${position.x}_${position.y}_${direction}"),
    ): OptionalEntityPlacement<P> {
        val entity = OptionalPlacement(
            originalEntity = null,
            prototype = prototype,
            position = position,
            direction = direction,
            cost = cost,
            selected = selected
        )
        _placements.add(entity)
        return entity
    }

    fun <P : EntityPrototype> addPlacement(
        entity: Entity<P>,
        position: Position = entity.position,
        direction: Direction = entity.direction,
        cost: Double = 1.0,
        selected: Literal = cp.newBoolVar("selected_${entity.prototype.name}_${position.x}_${position.y}_${direction}"),
    ): OptionalEntityPlacement<P> {
        val optionalEntity = OptionalPlacement(
            originalEntity = entity,
            prototype = entity.prototype,
            position = position,
            direction = direction,
            cost = cost,
            selected = selected
        )
        _placements.add(optionalEntity)
        return optionalEntity
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


    fun solve(display: Boolean = true): PlacementSolution {
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
            it is OptionalEntityPlacement<*> && solver.booleanValue(it.selected)
        } as List<OptionalEntityPlacement<*>>

    fun getSelectedEntities(): List<EntityPlacement<*>> =
        model.placements.filter {
            solver.booleanValue(it.selected)
        }
}

fun PlacementSolution.toBlueprintEntities(existingToCopyFrom: SpatialDataStructure<BlueprintEntity>?): MutableSpatialDataStructure<BlueprintEntity> {
    val result = DefaultSpatialDataStructure<BlueprintEntity>()
    for (entity in getSelectedEntities()) {
        result.add(entity.toBlueprintEntity(existingToCopyFrom))
    }
    return result
}

fun <P : EntityPrototype, E : Entity<P>> getAllUnrotatedTilePlacements(
    prototypes: Iterable<P>,
    bounds: TileBoundingBox,
    allowPlacement: (E) -> Boolean,
    createEntity: (P, TilePosition) -> E,
): List<E> {
    val boundsPos = bounds.toBoundingBox()
    val tiles = bounds.toList()
    return prototypes.toSet().parallelStream().flatMap { prototype: P ->
        tiles.parallelStream().map { tile -> createEntity(prototype, tile) }
    }.filter { it.collisionBox in boundsPos && allowPlacement(it) }.toList()
}

fun <P : EntityPrototype> getAllUnrotatedTilePlacementsBasic(
    model: EntityPlacementModel,
    prototypes: Iterable<P>,
    bounds: TileBoundingBox,
): List<BasicEntity<P>> {
    return getAllUnrotatedTilePlacements(
        prototypes,
        bounds,
        allowPlacement = { model.canPlace(it) },
        createEntity = { prototype, tile -> prototype.basicPlacedAtTile(tile) }
    )
}
