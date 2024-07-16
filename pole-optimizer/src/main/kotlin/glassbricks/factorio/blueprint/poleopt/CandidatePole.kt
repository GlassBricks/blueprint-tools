package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import glassbricks.factorio.blueprint.roundOutToTileBbox
import java.util.concurrent.ConcurrentHashMap

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

public class CandidatePoleSet(
    public val entities: EntityMap,
    public val candidatePoles: SpatialDataStructure<CandidatePole>,
) {
    public fun getPoweredEntities(pole: CandidatePole): Sequence<Entity> {
        val prototype = pole.prototype
        val radius = prototype.supply_area_distance
        val range = BoundingBox.around(pole.position, radius).roundOutToTileBbox()
        return entities.getInArea(range).filter { it.prototype.usesElectricity }
    }

    public fun getNeighbors(pole: CandidatePole): Sequence<CandidatePole> =
        candidatePoles.getPosInCircle(pole.position, pole.prototype.maximum_wire_distance)
            .filter { it != pole && it.canConnectTo(pole) }

    public fun getPoweredByMap(): Map<Entity, Set<CandidatePole>> {
        val result = mutableMapOf<Entity, MutableSet<CandidatePole>>()
        for (entity in entities) {
            if (entity.prototype.usesElectricity) {
                result[entity] = ConcurrentHashMap.newKeySet()
            }
        }
        candidatePoles.parallelStream().forEach { pole ->
            getPoweredEntities(pole).forEach { entity ->
                result[entity]?.add(pole)
            }
        }
        return result
    }

}

/**
 * Creates a [CandidatePoleSet], with all possible poles that can be placed in the given bounding box.
 *
 * Note that existing poles in the entities map are considered overlapping with new candidate poles.
 */
public fun getCompleteCandidatePoleSet(
    entities: EntityMap,
    polesToConsider: Collection<ElectricPolePrototype>,
    bounds: BoundingBox,
    forceIncludeExistingPoles: Boolean
): CandidatePoleSet {
    val candPoles =
        polesToConsider.parallelStream().flatMap { prototype ->
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
    return CandidatePoleSet(entities, poleSet)
}
