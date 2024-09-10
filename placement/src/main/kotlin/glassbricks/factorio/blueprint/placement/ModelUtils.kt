package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.hasControlBehavior
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placedAtTileBasic
import glassbricks.factorio.blueprint.placement.belt.addInitialSolution
import glassbricks.factorio.blueprint.placement.beltcp.addBeltLinesFrom
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
        var addHeuristicInitialSolution = false
        var forceInitialSolution = false
    }


    class OptimizedPoles(
        val prototypes: List<ElectricPolePrototype>,
    ) {
        var enforcePolesConnected = false
        var useLabelConnectivity = false
        var poleRootRel: Vector = Vector(0.5, 0.5)
        var addExistingAsInitialSolution = false
    }

    var optimizeBeltLines: OptimizeBelts? = null
    var optimizePoles: OptimizedPoles? = null
    fun optimizeBeltLines(init: OptimizeBelts.() -> Unit = {}) {
        optimizeBeltLines = OptimizeBelts().apply(init)
    }

    fun optimizePoles(prototypes: List<ElectricPolePrototype>, init: OptimizedPoles.() -> Unit = {}) {
        optimizePoles = OptimizedPoles(prototypes).apply(init)
    }

    @JvmName("optimizePolesString")
    fun optimizePoles(prototypes: List<String>, init: OptimizedPoles.() -> Unit = {}) {
        val prototypes = prototypes.map { VanillaPrototypes.getAs<ElectricPolePrototype>(it) }
        optimizePoles(prototypes, init)
    }

    fun optimizePoles(vararg prototypes: String, init: OptimizedPoles.() -> Unit = {}) {
        require(prototypes.isNotEmpty())
        optimizePoles(prototypes.asList(), init)
    }


    val toKeep = mutableSetOf<BlueprintEntity>()

    fun keepEntitiesWithControlBehavior() {
        for (entity in origEntities) {
            if (entity.hasControlBehavior())
                toKeep.add(entity)
        }
    }

    inline fun keepIf(condition: (BlueprintEntity) -> Boolean) {
        for (entity in origEntities) {
            if (condition(entity)) toKeep.add(entity)
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
        val optimizeBeltLines = optimizeBeltLines

        val model = EntityPlacementModel()


        val bounds = origEntities.enclosingBox()

        val entitiesToAdd = origEntities.filter {
            if (optimizeBeltLines != null && (it.prototype is TransportBeltPrototype || it.prototype is UndergroundBeltPrototype)) {
                return@filter false
            }
            if (optimizePoles != null && it.prototype is ElectricPolePrototype) {
                return@filter false
            }
            true
        }

        logger.info { "Adding fixed placements" }
        for (entity in entitiesToAdd)
            model.addFixedPlacement(entity)


        val beltPlacements = if (optimizeBeltLines != null) {
            model.addBeltLinesFrom(origEntities)
        } else null

        // important that fixed placements added before poles
        if (optimizePoles != null) {
            logger.info { "Adding pole placements" }
            val placements = model.addPolePlacements(
                optimizePoles.prototypes, bounds = bounds.roundOutToTileBbox()
            ) {
                removeEmptyPolesDist2()
                removeIfParallel {
                    it.collisionBox.roundOutToTileBbox().any {
                        beltPlacements?.get(it)?.canBeEmpty == false
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
                    val existing = origEntities.getAtPoint(candidate.position).find {
                        it.prototype == candidate.prototype
                    }
                    model.cp.addLiteralHint(candidate.placement.selected, existing != null)
                }
            }
        }

        logger.info { "Setting costs" }

        for (placement in model.placements) {
            if (placement !is OptionalEntityPlacement<*>) continue
            val cost = entityCosts[placement.prototype] ?: continue
            placement.cost = cost
        }

        if (distanceCostFactor != 0.0) {
            val center = bounds.getRelPoint(distanceCostCenter ?: optimizePoles?.poleRootRel ?: Vector(0.5, 0.5))
            model.addDistanceCostFrom(center, distanceCostFactor)
        }

        if (optimizeBeltLines?.addHeuristicInitialSolution == true) {
            val force = optimizeBeltLines.forceInitialSolution
            beltPlacements!!.addInitialSolution(
                force = force,
                params = InitialSolutionParams(canPlace = { testEntity ->
                    origEntities.getColliding(testEntity).none {
                        it.prototype !is TransportBeltPrototype && it.prototype !is UndergroundBeltPrototype
                    }
                })
            )
        }

        model.exportCopySource = origEntities
        return model
    }
}

fun EntityPlacementModel.addDistanceCostFrom(position: Position, scale: Double = 1e-6) {
    for (entity in placements) if (entity is OptionalEntityPlacement<*>) {
        entity.cost += scale * entity.position.distanceTo(position)
    }
}
