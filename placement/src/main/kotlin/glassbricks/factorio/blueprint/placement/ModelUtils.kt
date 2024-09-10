package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.CircuitConnectable
import glassbricks.factorio.blueprint.entity.Container
import glassbricks.factorio.blueprint.entity.Furnace
import glassbricks.factorio.blueprint.entity.Inserter
import glassbricks.factorio.blueprint.entity.hasAnyCircuitConnections
import glassbricks.factorio.blueprint.findMatching
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placedAtTileBasic
import glassbricks.factorio.blueprint.placement.belt.BeltLineCosts
import glassbricks.factorio.blueprint.placement.belt.withOptimizedBeltLines
import glassbricks.factorio.blueprint.placement.beltcp.addBeltLinesFrom
import glassbricks.factorio.blueprint.placement.beltcp.solveBeltLinesAsInitialSolution
import glassbricks.factorio.blueprint.placement.ops.addEntityNudging
import glassbricks.factorio.blueprint.placement.ops.getItemTransportGraph
import glassbricks.factorio.blueprint.placement.ops.nudgingAllowed
import glassbricks.factorio.blueprint.placement.poles.addPolePlacements
import glassbricks.factorio.blueprint.placement.poles.enforceConnectedWithDag
import glassbricks.factorio.blueprint.placement.poles.enforceConnectedWithDistanceLabels
import glassbricks.factorio.blueprint.placement.poles.getRelPoint
import glassbricks.factorio.blueprint.placement.poles.getRootPolesNear
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.roundOutToTileBbox
import io.github.oshai.kotlinlogging.KotlinLogging


private val logger = KotlinLogging.logger {}
// for handling nudging (in the future, make sure preserveWithControlBehavior still works as expected)

class BpModelBuilder(val origEntities: SpatialDataStructure<BlueprintEntity>) {
    constructor(blueprint: Blueprint) : this(blueprint.entities)


    class OptimizeBelts {
        /**
         * If true, solution is added as initial solution to the cp model.
         *
         * If false, the heuristic solution is applied first.
         */
        var withCp = false
        var forceWithCp = false
    }


    class OptimizePoles(
        val prototypes: List<ElectricPolePrototype>,
    ) {
        var enforcePolesConnected = false
        var useLabelConnectivity = false
        var poleRootRel: Vector = Vector(0.5, 0.5)
        var addExistingAsInitialSolution = false
        var ignoreUnpoweredEntities = false
    }

    var optimizeBeltLines: OptimizeBelts? = null
    fun optimizeBeltLines(init: OptimizeBelts.() -> Unit = {}) {
        optimizeBeltLines = OptimizeBelts().apply(init)
    }

    var optimizePoles: OptimizePoles? = null
    fun optimizePoles(prototypes: List<ElectricPolePrototype>, init: OptimizePoles.() -> Unit = {}) {
        optimizePoles = OptimizePoles(prototypes).apply(init)
    }

    @JvmName("optimizePolesString")
    fun optimizePoles(prototypes: List<String>, init: OptimizePoles.() -> Unit = {}) {
        val prototypes = prototypes.map { VanillaPrototypes.getAs<ElectricPolePrototype>(it) }
        optimizePoles(prototypes, init)
    }

    fun optimizePoles(vararg prototypes: String, init: OptimizePoles.() -> Unit = {}) {
        require(prototypes.isNotEmpty())
        optimizePoles(prototypes.asList(), init)
    }


    val toKeep = mutableSetOf<BlueprintEntity>()

    fun keepEntitiesWithCircuitConnections() {
        keepIf { it is CircuitConnectable && it.hasAnyCircuitConnections() }
    }

    inline fun keepIf(condition: (BlueprintEntity) -> Boolean) {
        for (entity in origEntities) {
            if (condition(entity)) toKeep.add(entity)
        }
    }

    val toNudge = mutableSetOf<BlueprintEntity>()

    fun addSafeNudging() {
        for (entity in origEntities) if (entity.nudgingAllowed()
            && (entity is Inserter
                    || entity is Furnace
                    || entity is Container)
        ) {
            toNudge.add(entity)
        }
    }

    var entityCosts: Map<EntityPrototype, Double> = mutableMapOf()

    fun setEntityCosts(costs: Map<String, Double>, prototypes: BlueprintPrototypes = VanillaPrototypes) {
        entityCosts = costs.mapKeys { prototypes[it.key] ?: error("Prototype not found: ${it.key}") }
    }

    fun setEntityCosts(vararg costs: Pair<String, Double>, prototypes: BlueprintPrototypes = VanillaPrototypes) {
        setEntityCosts(costs.toMap(), prototypes)
    }

    var distanceCostCenter: Vector? = null

    /**
     * Doing this also helps with symmetry breaking.
     */
    var distanceCostFactor: Double = 0.0

    fun build(): EntityPlacementModel {
        logger.info { "Building model" }
        val optimizePoles = optimizePoles
        val optimizeBeltLinesWithCp = optimizeBeltLines?.withCp == true

        val bounds = origEntities.enclosingBox()
        val distCostPosition = bounds.getRelPoint(distanceCostCenter ?: optimizePoles?.poleRootRel ?: Vector(0.5, 0.5))

        fun getEntityCost(
            prototype: EntityPrototype,
            position: Position,
        ): Double {
            var baseCost = entityCosts[prototype] ?: 0.0
            if (distanceCostFactor != 0.0)
                baseCost += distanceCostFactor * distCostPosition.distanceTo(position)
            return baseCost
        }

        val entities = if (optimizeBeltLines?.withCp == false) {
            origEntities.withOptimizedBeltLines(BeltLineCosts { type, pos, _ ->
                getEntityCost(type.prototype, pos.tileCenter())
            })
        } else origEntities

        val model = EntityPlacementModel()


        val entitiesToAdd = entities.filter {
            if (optimizeBeltLinesWithCp && (it.prototype is TransportBeltPrototype || it.prototype is UndergroundBeltPrototype)) {
                return@filter false
            }
            if (optimizePoles != null && it.prototype is ElectricPolePrototype) {
                return@filter false
            }
            true
        }

        logger.info { "Adding fixed placements" }
        val (nudgeEntities, fixedEntities) = entitiesToAdd.partition { it in toNudge }
        for (entity in fixedEntities) {
            model.addFixedPlacement(entity)
        }
        if (nudgeEntities.isNotEmpty()) {
            model.addEntityNudging(nudgeEntities, getItemTransportGraph(entities))
        }


        val beltPlacements = if (optimizeBeltLinesWithCp) {
            model.addBeltLinesFrom(entities)
        } else null

        // important that fixed placements added before poles
        if (optimizePoles != null) {
            logger.info { "Adding pole placements" }
            val placements = model.addPolePlacements(
                optimizePoles.prototypes,
                bounds = bounds.roundOutToTileBbox(),
                ignoreUnpoweredEntities = optimizePoles.ignoreUnpoweredEntities
            ) {
                removeEmptyPolesDist2()
                if (beltPlacements != null) {
                    removeIfParallel {
                        it.collisionBox.roundOutToTileBbox().any {
                            beltPlacements[it]?.canBeEmpty == false
                        }
                    }
                }
            }
            if (optimizePoles.enforcePolesConnected) {
                val rootPoles = placements.getRootPolesNear(bounds.getRelPoint(optimizePoles.poleRootRel))
                if (optimizePoles.useLabelConnectivity) {
                    placements.enforceConnectedWithDistanceLabels(rootPoles)
                } else {
                    placements.enforceConnectedWithDag(rootPoles)
                }
            }
            if (optimizePoles.addExistingAsInitialSolution) {
                for (candidate in placements.poles) {
                    val existing = entities.getIntersectingPosition(candidate.position).find {
                        it.prototype == candidate.prototype
                    }
                    model.cp.addLiteralHint(candidate.placement.selected, existing != null)
                }
            }
        }

        logger.info { "Setting costs" }
        for (placement in model.placements) {
            if (placement is OptionalEntityPlacement<*>) {
                placement.cost = getEntityCost(placement.prototype, placement.position)
            }
        }

        logger.info { "Setting to keep" }
        for (entity in toKeep) {
            val placement = model.placements.findMatching(entity)
            if (placement is OptionalEntityPlacement<*>) {
                model.cp.addLiteralEquality(placement.selected, true)
            }
        }

        if (optimizeBeltLinesWithCp) {
            beltPlacements!!.solveBeltLinesAsInitialSolution(
                force = optimizeBeltLines!!.forceWithCp,
                canPlace = { beltType, position, direction ->
                    val testEntity = beltType.prototype.placedAtTileBasic(position, direction.to8wayDirection())
                    entities.getColliding(testEntity).none {
                        it.prototype !is TransportBeltPrototype && it.prototype !is UndergroundBeltPrototype
                    }
                }
            )
        }

        model.exportCopySource = entities
        return model
    }
}

fun EntityPlacementModel.addDistanceCostFrom(position: Position, scale: Double = 1e-6) {
    for (entity in placements) if (entity is OptionalEntityPlacement<*>) {
        entity.cost += scale * entity.position.distanceTo(position)
    }
}
