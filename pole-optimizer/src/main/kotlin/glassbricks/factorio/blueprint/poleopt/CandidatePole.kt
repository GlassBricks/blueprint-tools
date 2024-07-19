package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import glassbricks.factorio.blueprint.roundOutToTileBbox
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.streams.asSequence
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

public class CandidatePole(
    prototype: ElectricPolePrototype,
    position: Position,
    public var forceInclude: Boolean = false,
    public var cost: Double = 1.0
) : BasicEntityInfo<ElectricPolePrototype>(prototype, position) {

    override fun toString(): String =
        "CandidatePole(name=${prototype.name}, position=$position, cost=$cost)"
}

public fun CandidatePole.toEntity(): ElectricPole = ElectricPole(prototype, basicEntityJson(prototype.name, position))

public fun ElectricPole.toCandidatePole(): CandidatePole = CandidatePole(prototype, position)

private fun CandidatePole.canConnectTo(other: CandidatePole): Boolean {
    val distance = minOf(this.prototype.maximum_wire_distance, other.prototype.maximum_wire_distance)
    return this.position.squaredDistanceTo(other.position) <= distance * distance
}

public class PoleCoverProblem(
    public val entities: EntityMap,
    public val candidatePoles: MutableSpatialDataStructure<CandidatePole>,
) {
    public val coveredEntities: Map<CandidatePole, List<Entity>> = run {
        candidatePoles.parallelStream().map { pole ->
            pole to computePoweredEntities(pole).toList()
        }.asSequence().toMap()
    }

    public val poweredByMap: Map<Entity, List<CandidatePole>> = run {
        val result = mutableMapOf<Entity, MutableList<CandidatePole>>()
        for ((pole, entities) in coveredEntities) {
            for (entity in entities) result.getOrPut(entity, ::mutableListOf).add(pole)
        }
        result
    }

    public fun computePoweredEntities(pole: CandidatePole): Sequence<Entity> {
        val prototype = pole.prototype
        val radius = prototype.supply_area_distance
        val range = BoundingBox.around(pole.position, radius).roundOutToTileBbox()
        return entities.getInArea(range).filter { it.prototype.usesElectricity }
    }

    public fun computeNeighbors(pole: CandidatePole): Sequence<CandidatePole> =
        candidatePoles.getPosInCircle(pole.position, pole.prototype.maximum_wire_distance)
            .filter { it != pole && it.canConnectTo(pole) }

    private var neighborsMap: MutableMap<CandidatePole, HashSet<CandidatePole>>? = null

    private fun computeNeighborsMap(): HashMap<CandidatePole, HashSet<CandidatePole>> {
        logger.info { "Computing neighbors map" }
        val (map, time) = measureTimedValue {
            candidatePoles.parallelStream().map { pole ->
                pole to computeNeighbors(pole).toHashSet()
            }.toList().toMap(HashMap())
        }
        logger.info { "Neighbors map computed in $time" }
        return map
    }

    public fun getNeighborsMap(): Map<CandidatePole, Set<CandidatePole>> =
        neighborsMap ?: computeNeighborsMap().also { neighborsMap = it }

    public fun updatePolesRemoved() {
        val neighborsMap = neighborsMap ?: return
        neighborsMap.keys.retainAll(candidatePoles)
        neighborsMap.values.forEach { it.retainAll(candidatePoles) }
    }
}

/**
 * Creates a [PoleCoverProblem], with all possible poles that can be placed in the given bounding box.
 *
 * Note that existing poles in the entities map are considered overlapping with new candidate poles.
 */
public fun createPoleCoverProblem(
    entities: EntityMap,
    polesToAdd: Collection<ElectricPolePrototype>,
    bounds: BoundingBox,
    forceIncludeExistingPoles: Boolean = true,
    processPoles: MutableSpatialDataStructure<CandidatePole>.() -> Unit = {}
): PoleCoverProblem {
    val candPoles =
        polesToAdd.parallelStream().flatMap { prototype ->
            bounds.roundOutToTileBbox().iterateTiles()
                .toList().parallelStream()
                .map { tile -> CandidatePole(prototype, prototype.tileSnappedPosition(tile)) }
                .filter { result -> result.collisionBox in bounds && !entities.getColliding(result).any() }
        }.toList()
    val poleSet = DefaultSpatialDataStructure(candPoles)
    if (forceIncludeExistingPoles) {
        for (entity in entities) if (entity is ElectricPole) {
            poleSet.add(
                entity.toCandidatePole().apply { forceInclude = true }
            )
        }
    }
    poleSet.processPoles()
    return PoleCoverProblem(entities, poleSet)
}


public fun PoleCoverProblem.removeEmptyPoles() {
    candidatePoles.removeIf { pole -> coveredEntities[pole]!!.isEmpty() }
    updatePolesRemoved()
}

/**
 * Removes if distance >1 to a pole that powers something.
 */
public fun PoleCoverProblem.removeEmptyPolesReach1() {
    val neighborsMap = getNeighborsMap()
    logger.info { "Removing empty poles" }
    val powersSomething = candidatePoles.filter { pole -> coveredEntities[pole]!!.isNotEmpty() }.toHashSet()
    candidatePoles.removeIf { pole ->
        pole !in powersSomething && neighborsMap[pole]!!.none { it in powersSomething }
    }
    updatePolesRemoved()
    logger.info { "Empty poles removed" }
}
