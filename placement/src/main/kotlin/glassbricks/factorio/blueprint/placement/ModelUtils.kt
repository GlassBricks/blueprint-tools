package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.hasControlBehavior
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.belt.InitialSolutionParams
import glassbricks.factorio.blueprint.placement.belt.addBeltLinesFrom
import glassbricks.factorio.blueprint.placement.belt.addInitialSolution
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

    var optimizeBeltLines = false
    var optimizePoles: List<ElectricPolePrototype>? = null
    var enforcePolesConnected = false
    var useLabelConnectivity = false
    var poleRootRel: Vector = Vector(0.5, 0.5)

    var addBeltInitialSolution = true


    val toKeep = mutableSetOf<BlueprintEntity>()

    fun keepWithControlBehavior() {
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

    var distanceCostCenter: Vector? = null

    /**
     * Doing this also helps with symmetry breaking.
     */
    var distanceCostFactor: Double = 0.0

    fun build(): EntityPlacementModel {
        logger.info { "Building model" }
        val model = EntityPlacementModel()
        val beltPlacements = if (optimizeBeltLines) {
            model.addBeltLinesFrom(origEntities)
        } else null
        val bounds = origEntities.enclosingBox()

        val entitiesToAdd = origEntities.filter {
            if (optimizeBeltLines && (it.prototype is TransportBeltPrototype || it.prototype is UndergroundBeltPrototype)) {
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

        // fixed placements added first, so that poles work

        if (optimizePoles != null) {
            logger.info { "Adding pole placements" }
            val placements = model.addPolePlacements(
                optimizePoles!!, bounds = bounds.roundOutToTileBbox()
            ) {
                removeEmptyPolesDist2()
                removeIfParallel {
                    it.collisionBox.roundOutToTileBbox().any {
                        beltPlacements?.get(it)?.canBeEmpty == false
                    }
                }
            }
            if (enforcePolesConnected) {
                val rootPoles = placements.getRootPolesNear(bounds.getRelPoint(poleRootRel))
                if (!useLabelConnectivity) {
                    placements.enforceConnectedWithDag(rootPoles)
                } else {
                    placements.enforceConnectedWithDistanceLabels(rootPoles)
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
            val center = bounds.getRelPoint(distanceCostCenter ?: poleRootRel)
            model.addDistanceCostFrom(center, distanceCostFactor)
        }

        if (optimizeBeltLines && addBeltInitialSolution) {
            beltPlacements!!.addInitialSolution(params = InitialSolutionParams(
                canUseTile = r@{
                    val collidesWithOriginal = origEntities.getInTile(it).any {
                        it.prototype !is TransportBeltPrototype && it.prototype !is UndergroundBeltPrototype
                    }
                    !collidesWithOriginal
                }
            ))
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
