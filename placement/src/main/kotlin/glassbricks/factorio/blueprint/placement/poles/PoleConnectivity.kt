package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.placement.GraphLike
import glassbricks.factorio.blueprint.placement.dijkstras
import io.github.oshai.kotlinlogging.KotlinLogging

typealias DistanceMetric = (PoleCandidate, PoleCandidate) -> Double

private val logger = KotlinLogging.logger {}

fun PolePlacements.getPoleGraph(
    distanceMetric: (PoleCandidate, PoleCandidate) -> Double = ::defaultConnectivityDistanceMetric,
): GraphLike<PoleCandidate> =
    object : GraphLike<PoleCandidate> {
        override val nodes: Collection<PoleCandidate> = poles
        override fun hasEdge(from: PoleCandidate, to: PoleCandidate): Boolean =
            from.neighbors.contains(to)

        override fun edgeWeight(from: PoleCandidate, to: PoleCandidate): Double = distanceMetric(from, to)
        override fun neighborsOf(node: PoleCandidate): Iterable<PoleCandidate> = node.neighbors
    }

/**
 * Each node is first weighted by distance, then inversely weighted by the number of things it powers.
 */
fun defaultConnectivityDistanceMetric(
    a: PoleCandidate,
    b: PoleCandidate,
): Double {
    val distance = a.position.distanceTo(b.position)
    val neighbors = minOf(a.poweredEntities.size, b.poweredEntities.size)
    return (distance + 1.5) / (neighbors + 0.3)
}

fun PolePlacements.enforceConnectedWithDag(
    rootPoles: Collection<PoleCandidate>,
    distanceMetric: DistanceMetric = ::defaultConnectivityDistanceMetric,
) {
    require(rootPoles.isNotEmpty()) {
        "Adding connectivity constraints without any root poles is the same as not adding any constraints"
    }
    val poleGraph = getPoleGraph(distanceMetric)
    val distances = dijkstras(poleGraph, rootPoles).distances
    if (poleGraph.nodes.any { it !in distances }) {
        logger.warn { "Some poles are not reachable from the root poles" }
    }
    for (pole in poles) {
        val distance = distances[pole] ?: continue
        val closerNeighbors = pole.neighbors.filter { neighbor ->
            distances[neighbor].let { it != null && it < distance }
        }
        if (closerNeighbors.isEmpty()) continue
        model.cp.addBoolOr(closerNeighbors.map { it.placement.selected })
            .onlyEnforceIf(pole.placement.selected)
    }
}

/**
 * More optimal guarantees than DAG version, but is exponentially slower.
 * Only recommended for small problems when the heuristics of [enforceConnectedWithDag] is not enough.
 */
fun PolePlacements.enforceConnectedWithDistanceLabels(
    rootPoles: Collection<PoleCandidate>,
    maxPoleDistance: Long = 200,
) {
    require(rootPoles.isNotEmpty()) {
        "Adding connectivity constraints without any root poles is the same as not adding any constraints"
    }
    val rootPoles = rootPoles.toSet()
    val cp = model.cp
    val poleDistance = poles.associateWith {
        cp.newIntVar(0, maxPoleDistance, it.prototype.name)
    }
    for (pole in rootPoles) {
        cp.addEquality(poleDistance[pole], 0)
    }
    for (pole in poles) if (pole !in rootPoles) {
        if (pole.neighbors.isEmpty()) continue
        val neighborIsSelected = pole.neighbors.map { neighbor ->
            cp.newBoolVar(neighbor.prototype.name).also { neighborSelected ->
                cp.addImplication(neighborSelected, pole.placement.selected)
                cp.addGreaterThan(poleDistance[pole]!!, poleDistance[neighbor]!!).onlyEnforceIf(neighborSelected)
            }
        }
        cp.addAtLeastOne(neighborIsSelected)
            .onlyEnforceIf(pole.placement.selected)
    }
}

fun PolePlacements.getRootPolesNear(point: Position): Set<PoleCandidate> {
    return poles.getPosInCircle(point, 10.0)
        .toMutableList()
        .apply { sortBy { it.position.squaredDistanceTo(point) } }
        .let { getMaximalClique(it) }
}

private fun getMaximalClique(poles: List<PoleCandidate>) = buildSet {
    for (pole in poles) {
        if (pole.neighbors.containsAll(this)) this.add(pole)
    }
}

fun Position.emul(other: Vector): Position = Position(x * other.x, y * other.y)
fun BoundingBox.getRelPoint(rel: Vector): Position = leftTop + size.emul(rel)
