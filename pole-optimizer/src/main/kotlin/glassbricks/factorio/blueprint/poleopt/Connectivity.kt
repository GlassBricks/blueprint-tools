package glassbricks.factorio.blueprint.poleopt

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Vec2d
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.measureTimedValue


public typealias CandidatePoleGraph = WeightedUnGraph<CandidatePole>
public typealias DistanceMetric = (CandidatePole, CandidatePole) -> Double

private val logger = KotlinLogging.logger {}

public fun PoleCoverProblem.createMaximallyConnectedPoleGraph(distanceMetric: (CandidatePole, CandidatePole) -> Double): CandidatePoleGraph {
    val neighborsMap = getNeighborsMap()
    val (graph, time) = measureTimedValue {
        logger.info { "Creating maximally connected pole graph" }
        val graph = CandidatePoleGraph(candidatePoles.toList())
        for (pole in candidatePoles) {
            for (neighbor in neighborsMap[pole]!!) {
                graph.addEdge(pole, neighbor, distanceMetric(pole, neighbor))
            }
        }
        graph
    }
    logger.info { "Created maximally connected pole graph in $time" }
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

    public val distances: DijkstrasResult<CandidatePole> = dijkstras(poleGraph, rootPoles)
    public fun addConstraints() {
        if (distances.distancesArr.any { it.isInfinite() }) {
            logger.warn { "Not all poles are reachable from the root poles" }
        }

        logger.info { "Adding connectivity constraints" }

        for (pole in problem.poles.candidatePoles) {
            val distance = distances.distancesMap[pole] ?: continue
            if (distance == 0.0) continue
            val closerNeighbors = poleGraph.neighborsOf(pole).filter { neighbor ->
                distances.distancesArr[neighbor.nodeIndex] < distance
            }
            if (closerNeighbors.isEmpty()) continue
            // pole chosen -> one of neighbors chosen
            val poleVar = problem.poleVariables[pole]!!
            val vars = ArrayList<ILPLikeSolver.Literal>(closerNeighbors.size + 1).apply {
                closerNeighbors.mapTo(this) {
                    val neighborPole = poleGraph.nodes[it.nodeIndex]
                    problem.poleVariables[neighborPole]!!
                }
                add(poleVar)
            }
            problem.solver.addDisjunction(vars)
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
                .toMutableList()
                .apply { sortBy { it.position.squaredDistanceTo(centerPoint) } }
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
    poles: Iterable<CandidatePole>,
) = buildList {
    for (pole in poles) {
        if (this.all { graph.hasEdge(it, pole) })
            this.add(pole)
    }
}

private fun Position.emul(other: Vec2d): Position = Position(x * other.x, y * other.y)
