package glassbricks.factorio.blueprint.placement

import com.google.ortools.Loader
import com.google.ortools.sat.*
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.round

private val logger = KotlinLogging.logger {}

class EntityPlacementModel(
    private val _placements: MutableSpatialDataStructure<EntityPlacement<*>> = DefaultSpatialDataStructure(),
    val cp: CpModel = CpModel(),
) {
    val solver: CpSolver = CpSolver()

    val placements: SpatialDataStructure<EntityPlacement<*>> get() = _placements
    var exportCopySource: SpatialDataStructure<BlueprintEntity>? = null

    fun canPlace(entity: Entity<*>): Boolean =
        placements.getColliding(entity).none { it.isFixed }


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

    /**
     * This makes the following simplifying assumptions:
     * - Only one entity can occupy a tile; if an entity occupies a tile, it occupies _all_ of the tile
     * - placeable-off-grid is ignored
     * - Collision masks are ignored (everything collides with everything)
     * - If fixed entities overlap, we still allow it (handles some edge cases due to assumptions from above)
     */
    private fun addNonOverlappingConstraint() {
        for (tile in placements.occupiedTiles()) {
            val entities = placements.getInTile(tile).filterTo(mutableListOf()) { it.isSimpleCollisionBox }
            if (entities.any { it.isFixed }) continue
            cp.addAtMostOne(entities.map { it.selected })
        }
        for (entity in placements) {
            if (!entity.isFixed && !entity.isSimpleCollisionBox) {
                throw NotImplementedError("Optional placements of rails")
            }
            if (entity.isFixed) {
                val colliding = placements.getColliding(entity)
                for (other in colliding) {
                    if (!other.isFixed) {
                        cp.addLiteralEquality(other.selected, false)
                    }
                }
            }
        }
    }

    /**
     * Costs are rounded to the nearest fraction of this value; e.g 20 means provide 1/20th of a cost unit.
     *
     * Set to Int.MAX_VALUE to disable rounding.
     */
    var costResolution: Int = 500

    private fun setObjective() {
        val entities = placements
            .filterIsInstance<OptionalEntityPlacement<*>>()
        val vars = entities.map { it.selected }.toTypedArray()
        val costs = entities.map {
            round(it.cost * costResolution) / costResolution
        }.toDoubleArray()
        cp.minimize(DoubleLinearExpr(vars, costs, 0.0))
    }


    fun solve(
        display: Boolean = true,
        optimize: Boolean = true,
    ): PlacementSolution {
        beginSolve(optimize, display)
        val status = solver.solve(cp)
        return PlacementSolution(model = this, status = status, solver = solver)
    }

    fun solveFlow(
        display: Boolean = true,
        optimize: Boolean = true,
    ): Flow<List<OptionalEntityPlacement<*>>> {
        beginSolve(optimize, display)
        return getSolutionFlow(solver, this)
    }

    private fun beginSolve(
        optimize: Boolean,
        display: Boolean,
    ) {
        logger.info { "Adding final constraints" }
        addNonOverlappingConstraint()
        if (optimize) setObjective()

        if (display) {
            solver.parameters.apply {
                logSearchProgress = true
                logToStdout = true
            }
        }
        if (placements.size > 10_000) attemptAggressiveGc()
        logger.info { "Starting cp solve" }
    }

    private fun getSolutionFlow(solver: CpSolver, model: EntityPlacementModel): Flow<List<OptionalEntityPlacement<*>>> =
        callbackFlow {
            val optionals = model.placements.filterIsInstance<OptionalEntityPlacement<*>>()
            val callback = object : CpSolverSolutionCallback() {
                override fun onSolutionCallback() {
                    try {
                        if (isClosedForSend) {
                            stopSearch()
                            return
                        }
                        val result = optionals.filter { booleanValue(it.selected) }
                        if (trySend(result).isFailure) {
                            stopSearch()
                        }
                    } catch (e: Throwable) {
                        close(e)
                    }
                }
            }

            val status = solver.solve(cp, callback)

            if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE) {
                close(RuntimeException("Failed to find solution: status $status"))
            }
        }

    fun exportFromSelectedOptionals(optionals: List<OptionalEntityPlacement<*>>): MutableSpatialDataStructure<BlueprintEntity> {
        val result = DefaultSpatialDataStructure<BlueprintEntity>()
        for (entity in this.placements) if (entity.isFixed) {
            result.add(entity.toBlueprintEntity(exportCopySource))
        }
        for (entity in optionals) {
            result.add(entity.toBlueprintEntity(exportCopySource))
        }
        return result
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

    fun export(): MutableSpatialDataStructure<BlueprintEntity> {
        val result = DefaultSpatialDataStructure<BlueprintEntity>()
        for (entity in getSelectedEntities()) {
            result.add(entity.toBlueprintEntity(model.exportCopySource))
        }
        return result
    }
}

fun <P : EntityPrototype, E : Entity<P>> getAllUnrotatedTilePlacements(
    prototypes: Iterable<P>,
    bounds: TileBoundingBox,
    allowPlacement: (E) -> Boolean,
    createEntity: (P, TilePosition) -> E,
): List<E> {
    val boundsPos = bounds.toBoundingBox()
    val tiles = bounds.toList()
    logger.info { "Generating placements for ${prototypes.take(5).joinToString { it.name }}" }
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
        createEntity = { prototype, tile -> prototype.placedAtTileBasic(tile) }
    )
}


fun attemptAggressiveGc() {
    logger.info { "Attempting to achieve full GC" }
    val r = Runtime.getRuntime()
    r.gc()
    var f = r.freeMemory()
    var m = r.maxMemory()
    var t = r.totalMemory()
    repeat(10) {
        r.gc()
        Thread.sleep(10)
        val f2 = r.freeMemory()
        val m2 = r.maxMemory()
        val t2 = r.totalMemory()
        if (f == f2 && m == m2 && t == t2) {
            logger.info { "Achieved full GC" }
            return
        }
        f = f2
        m = m2
        t = t2
    }
    logger.warn { "Failed to achieve full GC" }
}
