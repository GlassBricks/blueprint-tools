package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.TileBoundingBox
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.placement.EntityPlacement
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.getAllPossibleUnrotatedPlacements
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import glassbricks.factorio.blueprint.roundOutToTileBbox
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

public typealias PolePlacement = EntityPlacement<ElectricPolePrototype>

public fun PolePlacement.toEntity(): ElectricPole = ElectricPole(prototype, basicEntityJson(prototype.name, position))

private fun PolePlacement.canConnectTo(other: PolePlacement): Boolean {
    val distance = minOf(this.prototype.maximum_wire_distance, other.prototype.maximum_wire_distance)
    return this.position.squaredDistanceTo(other.position) <= distance * distance
}

public class PolePlacementOptions(
    /**
     * Remove poles that are not connected to anything, and don't connect to another pole that is connected to something.
     */
    public var removeEmptyPolesReach1: Boolean = false,
    /** Automatically call [PolePlacements.addConstraints]. */
    public var addToModel: Boolean = true
)

/**
 * Handles constraints for to correctly placing poles/powering entities.
 */
public class PolePlacements(
    public val model: EntityPlacementModel,
    options: PolePlacementOptions = PolePlacementOptions()
) {
    public val poles: Set<PolePlacement>
    public val coveredEntities: Map<PolePlacement, List<EntityPlacement<*>>>
    public val poweringPoles: Map<EntityPlacement<*>, List<PolePlacement>>

    init {
        @Suppress("UNCHECKED_CAST")
        val poles =
            model.placements.filterTo(HashSet()) { it.prototype is ElectricPolePrototype } as HashSet<PolePlacement>
        val coveredEntities =
            poles.parallelStream()
                .map { pole -> pole to computePoweredEntities(model, pole) }
                .toList()
                .toMap(HashMap())
        val poweredMap = buildMap<_, MutableList<PolePlacement>> {
            for ((pole, entities) in coveredEntities) {
                for (entity in entities) getOrPut(entity, ::mutableListOf).add(pole)
            }
        }
        this.poles = poles
        this.coveredEntities = coveredEntities
        this.poweringPoles = poweredMap

        if (options.removeEmptyPolesReach1) {
            val neighborsMap = this.computeNeighborsMap().also { _neighborsMap = it }
            logger.info { "Removing empty poles" }
            val removeTime = measureTime {
                val powersSomething =
                    poles.filterTo(HashSet()) { pole -> coveredEntities[pole]!!.isNotEmpty() }

                val toRemove = poles.filterTo(HashSet()) { pole ->
                    !(pole in powersSomething || neighborsMap[pole]!!.any { it in powersSomething })
                }
                model.removeAll(toRemove)
                poles.removeAll(toRemove)
                neighborsMap.keys.retainAll(poles)
                neighborsMap.values.forEach { it.retainAll(poles) }
                coveredEntities.keys.retainAll(poles)
            }
            logger.info { "Removed empty poles in $removeTime" }
        }
        if (options.addToModel) {
            addConstraints()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun computeNeighbors(pole: PolePlacement): MutableList<PolePlacement> =
        model.placements.getPosInCircle(pole.position, pole.prototype.maximum_wire_distance)
            .filterTo(ArrayList()) {
                it.prototype is ElectricPolePrototype && it != pole && (it as PolePlacement).canConnectTo(pole)
            }.let {
                it as MutableList<PolePlacement>
            }


    private var constraintsAdded = false
    public fun addConstraints() {
        if (constraintsAdded) return
        constraintsAdded = true
        for ((entity, poles) in poweringPoles) {
            model.cpModel.addAtLeastOne(poles.map { it.selected })
                .onlyEnforceIf(entity.selected)
        }
    }


    private var _neighborsMap: MutableMap<PolePlacement, MutableList<PolePlacement>>? = null
    public val neighborsMap: Map<PolePlacement, List<PolePlacement>>
        get() = _neighborsMap ?: computeNeighborsMap().also { _neighborsMap = it }

    private fun computeNeighborsMap(): HashMap<PolePlacement, MutableList<PolePlacement>> {
        logger.info { "Computing neighbors map" }
        val (map, time) = measureTimedValue {
            poles.parallelStream().map { pole -> pole to computeNeighbors(pole) }
                .toList()
                .toMap(HashMap())
        }
        logger.info { "Neighbors map computed in $time" }
        return map
    }

    private fun computePoweredEntities(
        model: EntityPlacementModel,
        pole: Entity<ElectricPolePrototype>
    ): List<EntityPlacement<*>> {
        val range = BoundingBox.around(pole.position, pole.prototype.supply_area_distance)
            .roundOutToTileBbox()
        return model.placements.getInArea(range).filterTo(ArrayList()) { it.prototype.usesElectricity }
    }
}

/**
 * Adds all possible poles that can be placed in the given bounding box, with the given prototypes.
 *
 * Considers existing poles ([FixedEntity]) as well.
 */
public fun EntityPlacementModel.addPolePlacements(
    polesToAdd: Iterable<ElectricPolePrototype>,
    bounds: TileBoundingBox,
    options: PolePlacementOptions = PolePlacementOptions()
): PolePlacements {
    val allPoles: MutableList<Entity<ElectricPolePrototype>> =
        getAllPossibleUnrotatedPlacements(polesToAdd, bounds)
            .toMutableList()

    for (pole in allPoles) {
        this.addPlacement(pole)
    }

    return PolePlacements(this, options)
}