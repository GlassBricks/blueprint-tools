package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.placement.CalculatedMapGraph
import glassbricks.factorio.blueprint.placement.DijkstrasResult
import glassbricks.factorio.blueprint.placement.dijkstras
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import io.github.oshai.kotlinlogging.KotlinLogging


typealias CandidatePoleGraph = CalculatedMapGraph<PolePlacement>
typealias DistanceMetric = (PolePlacement, PolePlacement) -> Double

private val logger = KotlinLogging.logger {}

fun PolePlacements.getPoleGraph(distanceMetric: (PolePlacement, PolePlacement) -> Double): CandidatePoleGraph =
    CalculatedMapGraph(neighborsMap, distanceMetric)

fun euclidianDistancePlus(amt: Double): DistanceMetric = { a, b ->
    a.position.distanceTo(b.position) + amt
}

fun favorPolesThatPowerMore(
    polePlacements: PolePlacements,
): DistanceMetric = { a, b ->
    val aNeighbors = polePlacements.coveredEntities[a]?.size ?: 0
    val bNeighbors = polePlacements.coveredEntities[b]?.size ?: 0
    val euclidianDistance = a.position.distanceTo(b.position)
    euclidianDistance / (aNeighbors + bNeighbors + 5)
}

class DistanceDAGConnectivity(
    private val polePlacements: PolePlacements,
    rootPoles: List<PolePlacement>,
    distanceMetric: DistanceMetric
) {
    private val poleGraph = polePlacements.getPoleGraph(distanceMetric)

    init {
        require(rootPoles.isNotEmpty()) {
            "Adding connectivity constraints without any root poles is the same as not adding any constraints"
        }
    }

    val distances: DijkstrasResult<PolePlacement> = dijkstras(poleGraph, rootPoles)
    fun addConstraints() {
        if (poleGraph.nodes.any { it !in distances.distances }) {
            logger.warn { "Not all poles are reachable from the root poles" }
        }

        logger.info { "Adding connectivity constraints" }

        for (pole in polePlacements.poles) {
            val distance = distances.distances[pole] ?: continue
            if (distance == 0.0) continue
            val closerNeighbors = poleGraph.neighborsOf(pole).filter { neighbor ->
                distances.distances[neighbor].let { it != null && it < distance }
            }
            if (closerNeighbors.isEmpty()) continue

            polePlacements.model.cpModel.addBoolOr(closerNeighbors.map { it.selected })
                .onlyEnforceIf(pole.selected)
        }
    }
}

/**
 * More exact connectivity guarantees than the DAG version, but is exponentially slower.
 * Only recommended for very small problems when the heuristics of [DistanceDAGConnectivity] is not enough.
 */
class DistanceLabelConnectivity(
    private val polePlacements: PolePlacements,
    private val rootPoles: Iterable<PolePlacement>,
    private val maxPoleDistance: Long = 500,
) {
    // might be rather slow... we'll see
    fun addConstraints() {
        val rootPoles = rootPoles.toSet()
        val cp = polePlacements.model.cpModel
        val poleDistanceVars = polePlacements.poles.associateWith {
            cp.newIntVar(0, maxPoleDistance, it.prototype.name)
        }
        for (pole in rootPoles) {
            cp.addEquality(poleDistanceVars[pole], 0)
        }
        for (pole in polePlacements.poles) {
            if (pole in rootPoles) continue
            val neighbors = polePlacements.neighborsMap[pole]!!
            if (neighbors.isEmpty()) continue
            val thisDistance = poleDistanceVars[pole]!!
            val neighborIsSelected = neighbors.map { cp.newBoolVar(it.prototype.name) }
            cp.addAtLeastOne(neighborIsSelected + !pole.selected)

            for ((neighbor, neighborSelected) in neighbors.zip(neighborIsSelected)) {
                cp.addImplication(neighborSelected, neighbor.selected)
                val neighborDistance = poleDistanceVars[neighbor]!!
                cp.addGreaterThan(thisDistance, neighborDistance).onlyEnforceIf(neighborSelected)
            }
        }
    }
}

fun PolePlacements.getRootPolesNear(relativePos: Vector): List<PolePlacement> {
    val boundingBox = model.placements.enclosingBox()
    val centerPoint = boundingBox.leftTop + boundingBox.size.emul(relativePos)
    return model.placements.getPosInCircle(centerPoint, 8.0)
        .filter { it.prototype is ElectricPolePrototype }
        .toMutableList()
        .let {
            @Suppress("UNCHECKED_CAST")
            it as MutableList<PolePlacement>
        }
        .apply { sortBy { it.position.squaredDistanceTo(centerPoint) } }
        .let { getMaximalClique(this, it) }
}

fun PolePlacements.getExistingPoles(): List<PolePlacement> {
    @Suppress("UNCHECKED_CAST")
    return model.placements.filter { it.isFixed && it.prototype is ElectricPolePrototype }
            as List<PolePlacement>
}

fun PolePlacements.rootPolesFromExistingOrNear(relativePos: Vector): List<PolePlacement> {
    return getExistingPoles().ifEmpty {
        getRootPolesNear(relativePos)
    }
}

private fun getMaximalClique(
    placements: PolePlacements,
    poles: List<PolePlacement>
) = buildList {
    for (pole in poles) {
//        if (this.all { graph.hasEdge(it, pole) })
        val neighbors = placements.neighborsMap[pole]!!
        if (neighbors.containsAll(this))
            this.add(pole)
    }
}

private fun Position.emul(other: Vector): Position = Position(x * other.x, y * other.y)
