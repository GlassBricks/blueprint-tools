package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.model.EntityMap
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.prototypes.usesElectricity

public typealias CandidatePoleLocation = EntitySpatial<ElectricPolePrototype>

public class CandidatePole(
    position: Position,
    prototype: ElectricPolePrototype,
    public var poweredEntities: Set<Entity> = emptySet(),
    public var neighborPoles: Set<CandidatePole> = emptySet(),
    public var cost: Double = 1.0
) : EntitySpatial<ElectricPolePrototype>(prototype, position) {
    override fun toString(): String =
        "CandidatePole(name=${prototype.name}, position=$position, ${poweredEntities.size} powered, ${neighborPoles.size} neighbors)"
}

private fun CandidatePole.canConnectTo(other: CandidatePole): Boolean {
    val distance = minOf(this.prototype.maximum_wire_distance, other.prototype.maximum_wire_distance)
    return this.position.squaredDistanceTo(other.position) <= distance * distance
}

public class CandidatePoleSet(
    public val poles: SpatialDataStructure<CandidatePole>,
    /** If entity is not powered by anything, returns null. */
    public val poweredBy: Map<Entity, Set<CandidatePole>>
)

public fun createCandidatePoleSet(
    entitiesToPower: EntityMap,
    poleLocations: Iterable<CandidatePoleLocation>
): CandidatePoleSet {
    val poleMap = DefaultSpatialDataStructure<CandidatePole>()
    val poweredBy = mutableMapOf<Entity, MutableSet<CandidatePole>>()
    for (pole in poleLocations) {
        val radius = pole.prototype.supply_area_distance
        val supplyArea = BoundingBox.around(pole.position, radius)
        val poweredEntities = entitiesToPower.getInArea(supplyArea)
            .filter { it.prototype.usesElectricity }
            .toSet()
        val candidatePole = CandidatePole(pole.position, pole.prototype, poweredEntities)
        poleMap.add(candidatePole)
        for (entity in poweredEntities) {
            poweredBy.getOrPut(entity, ::hashSetOf).add(candidatePole)
        }
    }
    for (pole in poleMap) {
        pole.neighborPoles = poleMap.getPosInCircle(pole.position, pole.prototype.maximum_wire_distance)
            .filter { it != pole && it.canConnectTo(pole) }.toSet()
    }
    return CandidatePoleSet(poleMap, poweredBy)
}

/**
 * Creates a [CandidatePoleSet], with all possible poles that can be placed in the given bounding box.
 */
public fun getCompleteCandidatePoles(
    entities: EntityMap,
    polesToConsider: Collection<ElectricPolePrototype>,
    bounds: BoundingBox,
): CandidatePoleSet {
    val poleLocations = polesToConsider.flatMap { prototype ->
        bounds.roundOutToTileBbox().iterateTiles().mapNotNull { tile ->
            val polePosition = prototype.tileSnappedPosition(tile) ?: return@mapNotNull null
            val result = CandidatePoleLocation(prototype, polePosition)
            if (result.collisionBox !in bounds || entities.getColliding(result).any()) return@mapNotNull null
            result
        }
    }
    return createCandidatePoleSet(entities, poleLocations)
}
