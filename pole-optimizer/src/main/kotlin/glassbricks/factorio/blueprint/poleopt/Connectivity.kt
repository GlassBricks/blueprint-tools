package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Vec2d


public typealias CandidatePoleGraph = WeightedGraph<CandidatePole>
public typealias DistanceMetric = (CandidatePole, CandidatePole) -> Double

public fun PoleCoverProblem.createMaximallyConnectedPoleGraph(distanceMetric: (CandidatePole, CandidatePole) -> Double): CandidatePoleGraph {
    val neighborsMap = getNeighborsMap()
    val graph = CandidatePoleGraph()
    for (pole in candidatePoles) graph.addNode(pole)
    for (pole in candidatePoles) {
        for (neighbor in neighborsMap[pole]!!) {
            graph.addEdge(pole, neighbor, distanceMetric(pole, neighbor))
        }
    }
    return graph
}

public fun euclidianDistancePlus(amt: Double): DistanceMetric = { a, b ->
    a.position.distanceTo(b.position) + amt
}

public class DistanceBasedConnectivity(
    public val problem: PoleCoverILPSolver,
    public val poleGraph: CandidatePoleGraph,
    public val rootPoles: List<CandidatePole>
) {

    public val distances: Map<CandidatePole, Double> = dijkstras(poleGraph, rootPoles).distances
    public fun addConstraints() {
        if (!distances.keys.containsAll(problem.poles.candidatePoles)) {
            println("Warning: not all poles are connected")
        }

        for (pole in problem.poles.candidatePoles) {
            val distance = distances[pole] ?: continue
            if (distance == 0.0) continue
            val closerNeighbors = poleGraph.neighborsOf(pole).filter { neighbor ->
                distances[neighbor] ?: Double.POSITIVE_INFINITY < distance
            }
            if (closerNeighbors.isEmpty()) continue
            // pole chosen -> one of neighbors chosen
            val neighborVars = closerNeighbors.map { problem.poleVariables[it]!! }
            val poleVar = problem.poleVariables[pole]!!
            problem.solver.addDisjunction(neighborVars.map { it.asTrue() } + poleVar.asFalse())
        }
    }

    public companion object {
        private fun getRootPolesNear(
            poles: PoleCoverProblem,
            graph: CandidatePoleGraph,
            relativePos: Vec2d
        ): List<CandidatePole> {
            val boundingBox = poles.entities.enclosingBox()
            val centerPoint = boundingBox.leftTop + boundingBox.size.emul(relativePos)
            return poles.candidatePoles.getPosInCircle(centerPoint, 8.0)
                .sortedBy { it.position.squaredDistanceTo(centerPoint) }
                .let { getMaximalClique(graph, it) }
        }

        public fun fromAroundPt(
            problem: PoleCoverILPSolver,
            relativePos: Vec2d,
            distanceMetric: (CandidatePole, CandidatePole) -> Double = euclidianDistancePlus(0.0)
        ): DistanceBasedConnectivity {
            val poleGraph = problem.poles.createMaximallyConnectedPoleGraph(distanceMetric)
            val rootPoles = getRootPolesNear(problem.poles, poleGraph, relativePos)
            return DistanceBasedConnectivity(problem, poleGraph, rootPoles)
        }
    }
}

private fun getMaximalClique(
    graph: CandidatePoleGraph,
    poles: Sequence<CandidatePole>,
) = buildList {
    for (pole in poles) {
        if (this.all { graph.hasEdge(it, pole) })
            this.add(pole)
    }
}

private fun Position.emul(other: Vec2d): Position = Position(x * other.x, y * other.y)
