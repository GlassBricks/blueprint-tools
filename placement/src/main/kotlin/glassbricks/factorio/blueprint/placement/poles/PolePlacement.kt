package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.TileBoundingBox
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.computeCollisionBox
import glassbricks.factorio.blueprint.placement.EntityPlacement
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.getAllUnrotatedTilePlacements
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import glassbricks.factorio.blueprint.roundOutToTileBbox
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

class PoleCandidate(
    override val prototype: ElectricPolePrototype,
    tilePosition: TilePosition,
) : Entity<ElectricPolePrototype> {
    override val direction: Direction get() = Direction.North
    override val position: Position = prototype.tileSnappedPosition(tilePosition)
    override val collisionBox: BoundingBox = computeCollisionBox(prototype, position, direction)

    @Volatile
    lateinit var poweredEntities: List<EntityPlacement<*>>
        internal set

    @Volatile
    internal lateinit var _neighbors: MutableList<PoleCandidate>
    val neighbors: List<PoleCandidate> get() = _neighbors

    lateinit var placement: OptionalEntityPlacement<ElectricPolePrototype>
        internal set

    override fun toString(): String = "PoleCandidate(${prototype.name}, $position)"
}


// refactoring of above
class PoleCandidates(
    allEntities: SpatialDataStructure<EntityPlacement<*>>,
    poleCandidates: List<PoleCandidate>,
) {
    val entitiesToPower = allEntities.filterTo(DefaultSpatialDataStructure()) { it.prototype.usesElectricity }
    val poles = DefaultSpatialDataStructure(poleCandidates)

    init {
        // compute covering entities
        logger.info { "Computing pole powered entities and neighbors" }
        poleCandidates.parallelStream().forEach { pole ->
            val poweredEntities = computePoweredEntities(pole)
            val neighbors = computeNeighbors(pole)
            // slightly better memory contention stuff
            pole.poweredEntities = poweredEntities
            pole._neighbors = neighbors
        }
    }

    val entityPoweredMap = buildMap {
        for (entity in entitiesToPower) {
            put(entity, ArrayList<PoleCandidate>())
        }
        for (pole in poles) for (entity in pole.poweredEntities) {
            this[entity]!!.add(pole)
        }
    }

    private fun computePoweredEntities(pole: Entity<ElectricPolePrototype>): List<EntityPlacement<*>> {
        val coveredArea = BoundingBox.around(
            pole.position, pole.prototype.supply_area_distance
        ).roundOutToTileBbox()
        return entitiesToPower.getInArea(coveredArea).toList()
    }

    private fun computeNeighbors(pole: PoleCandidate): MutableList<PoleCandidate> =
        poles.getPosInCircle(pole.position, pole.prototype.maximum_wire_distance)
            .filterTo(ArrayList()) { it != pole && pole.canConnectToPole(it) }

    fun removeIfParallel(predicate: (PoleCandidate) -> Boolean) {
        logger.info { "Removing poles" }
        val toRemove = poles.parallelStream().filter(predicate).collect(Collectors.toSet())
        poles.removeAll(toRemove)
        poles.parallelStream().forEach { it._neighbors.removeAll(toRemove) }
        entityPoweredMap.values.parallelStream().forEach { it.removeAll(toRemove) }
    }

    fun removeEmptyPolesDist2() {
        removeIfParallel { pole ->
            pole.poweredEntities.isEmpty() && pole.neighbors.all { it.poweredEntities.isEmpty() }
        }
    }

    fun addToModel(
        model: EntityPlacementModel,
        ignoreUnpoweredEntities: Boolean = false,
    ): PolePlacements {
        poles.forEach {
            it.placement = model.addPlacement(it)
        }
        for (entity in entitiesToPower) {
            val poweringPoles = entityPoweredMap[entity]!!.map { it.placement.selected }
            if (poweringPoles.isEmpty()) {
                if (!ignoreUnpoweredEntities) {
                    error("Entity $entity is not powered by any pole")
                }
                continue
            }
            model.cp.addAtLeastOne(poweringPoles).onlyEnforceIf(entity.selected)
        }
        return PolePlacements(model, poles)
    }
}

class PolePlacements(
    val model: EntityPlacementModel,
    val poles: SpatialDataStructure<PoleCandidate>,
)

fun Entity<ElectricPolePrototype>.canConnectToPole(other: Entity<*>): Boolean {
    val otherProto = other.prototype as? ElectricPolePrototype ?: return false
    val distance = minOf(this.prototype.maximum_wire_distance, otherProto.maximum_wire_distance)
    return this.position.squaredDistanceTo(other.position) <= distance * distance
}

fun EntityPlacementModel.createPoleCandidates(
    poles: Iterable<ElectricPolePrototype>,
    bounds: TileBoundingBox,
): PoleCandidates {
    val poleCandidates = getAllUnrotatedTilePlacements(poles,
        bounds,
        allowPlacement = { this.canPlace(it) },
        createEntity = { proto, pos -> PoleCandidate(proto, pos) })
    return PoleCandidates(placements, poleCandidates)
}

inline fun EntityPlacementModel.addPolePlacements(
    poles: Iterable<ElectricPolePrototype>,
    bounds: TileBoundingBox = placements.enclosingTileBox(),
    ignoreUnpoweredEntities: Boolean = false,
    configure: PoleCandidates.() -> Unit = {},
): PolePlacements {
    return createPoleCandidates(poles, bounds).apply(configure).addToModel(this, ignoreUnpoweredEntities)
}
