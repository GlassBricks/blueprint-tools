package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.entity.basicEntityJson
import glassbricks.factorio.blueprint.model.EntityMap
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import java.util.concurrent.ConcurrentHashMap

public class CandidatePole(
    prototype: ElectricPolePrototype,
    position: Position,
    public var cost: Double = 1.0
) : EntitySpatial<ElectricPolePrototype>(prototype, position) {

    public fun toEntity(): ElectricPole = ElectricPole(prototype, basicEntityJson(prototype.name, position))

    override fun toString(): String =
        "CandidatePole(name=${prototype.name}, position=$position, cost=$cost)"
}

private fun CandidatePole.canConnectTo(other: CandidatePole): Boolean {
    val distance = minOf(this.prototype.maximum_wire_distance, other.prototype.maximum_wire_distance)
    return this.position.squaredDistanceTo(other.position) <= distance * distance
}

public class CandidatePoleSet(
    public val entitiesToCover: EntityMap,
    public val poles: SpatialDataStructure<CandidatePole>,
) {
    public fun getPoweredEntities(pole: CandidatePole): Sequence<Entity> {
        val prototype = pole.prototype
        val radius = prototype.supply_area_distance
        val range = BoundingBox.around(pole.position, radius).roundOutToTileBbox()
        return entitiesToCover.getInArea(range).filter { it.prototype.usesElectricity }
    }

    public fun getNeighbors(pole: CandidatePole): Sequence<CandidatePole> =
        poles.getPosInCircle(pole.position, pole.prototype.maximum_wire_distance)
            .filter { it != pole && it.canConnectTo(pole) }

    public fun getPoweredByMap(): Map<Entity, Set<CandidatePole>> {
        val result = mutableMapOf<Entity, MutableSet<CandidatePole>>()
        for (entity in entitiesToCover) {
            if (entity.prototype.usesElectricity) {
                result[entity] = ConcurrentHashMap.newKeySet()
            }
        }
        poles.parallelStream().forEach { pole ->
            getPoweredEntities(pole).forEach { entity ->
                result[entity]?.add(pole)
            }
        }
        return result
    }

}

/**
 * Creates a [CandidatePoleSet], with all possible poles that can be placed in the given bounding box.
 */
public fun getCompleteCandidatePoleSet(
    entities: EntityMap,
    polesToConsider: Collection<ElectricPolePrototype>,
    bounds: BoundingBox,
): CandidatePoleSet {
    val allPoles =
        polesToConsider.parallelStream().flatMap { prototype ->
            bounds.roundOutToTileBbox().iterateTiles()
                .toList().parallelStream()
                .map { tile ->
                    CandidatePole(prototype, prototype.tileSnappedPosition(tile))
                }.filter { result ->
                    result.collisionBox in bounds && !entities.getColliding(result).any()
                }
        }.toList()
    val poles = DefaultSpatialDataStructure<CandidatePole>(allPoles)
    return CandidatePoleSet(entities, poles)
}
