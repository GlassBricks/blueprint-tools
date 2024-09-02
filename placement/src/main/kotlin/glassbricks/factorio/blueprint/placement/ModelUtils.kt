package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.hasControlBehavior
import glassbricks.factorio.blueprint.findMatching
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.ops.addBeltLinesFrom
import glassbricks.factorio.blueprint.placement.poles.addPolePlacements
import glassbricks.factorio.blueprint.placement.poles.enforceConnectedByDistanceLabels
import glassbricks.factorio.blueprint.placement.poles.enforceConnectedWithDag
import glassbricks.factorio.blueprint.placement.poles.getRelPoint
import glassbricks.factorio.blueprint.placement.poles.getRootPolesNear
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.roundOutToTileBbox


// for handling nudging (in the future, make sure preserveWithControlBehavior still works as expected)

class BpModelBuilder(val entities: SpatialDataStructure<BlueprintEntity>) {
    constructor(blueprint: Blueprint) : this(blueprint.entities)

    var optimizeBeltLines = false
    var optimizePoles: List<ElectricPolePrototype>? = null
    var enforcePolesConnected = false
    var useLabelConnectivity = false
    var poleRootRel: Vector = Vector(0.5, 0.5)

    var hintFromExising = true


    val toKeep = mutableSetOf<BlueprintEntity>()

    fun keepWithControlBehavior() {
        for (entity in entities) {
            if (entity.hasControlBehavior())
                toKeep.add(entity)
        }
    }

    inline fun keepIf(condition: (BlueprintEntity) -> Boolean) {
        for (entity in entities) {
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
        val model = EntityPlacementModel()
        val beltGrid = if (optimizeBeltLines) {
            model.addBeltLinesFrom(entities)
        } else null
        val bounds = entities.enclosingBox()

        val entitiesToAdd = entities.filter {
            if (optimizeBeltLines && (it.prototype is TransportBeltPrototype || it.prototype is UndergroundBeltPrototype)) {
                return@filter false
            }
            if (optimizePoles != null && it.prototype is ElectricPolePrototype) {
                return@filter false
            }
            true
        }

        for (entity in entitiesToAdd)
            model.addFixedPlacement(entity)

        // fixed placements added first, so that poles work

        if (optimizePoles != null) {
            val placements = model.addPolePlacements(
                optimizePoles!!, bounds = bounds.roundOutToTileBbox()
            ) {
                removeEmptyPolesDist2()
                removeIf {
                    it.collisionBox.roundOutToTileBbox().any {
                        beltGrid?.get(it)?.canBeEmpty == false
                    }
                }
            }
            if (enforcePolesConnected) {
                val rootPoles = placements.getRootPolesNear(bounds.getRelPoint(poleRootRel))
                if (!useLabelConnectivity) {
                    placements.enforceConnectedWithDag(rootPoles)
                } else {
                    placements.enforceConnectedByDistanceLabels(rootPoles)
                }
            }
        }

        for (entity in toKeep) {
            val existingEntity =
                model.placements.findMatching(entity) ?: error("Entity placement matching preserved entity not found")
            model.cp.addEquality(existingEntity.selected, true)
        }

        for (placement in model.placements) {
            if (placement !is OptionalEntityPlacement<*>) continue
            val cost = entityCosts[placement.prototype] ?: continue
            placement.cost = cost
        }

        if (distanceCostFactor != 0.0) {
            val center = bounds.getRelPoint(distanceCostCenter ?: poleRootRel)
            model.addDistanceCostFrom(center, distanceCostFactor)
        }

        if (hintFromExising) {
            for (placement in model.placements) if (placement is OptionalEntityPlacement<*>) {
                val existingEntity = entities.findMatching(placement)
                if (existingEntity != null) model.cp.addHint(placement.selected, true)
            }
        }

        return model
    }
}

fun EntityPlacementModel.addDistanceCostFrom(position: Position, scale: Double = 1e-6) {
    for (entity in placements) if (entity is OptionalEntityPlacement<*>) {
        entity.cost += scale * entity.position.distanceTo(position)
    }
}
