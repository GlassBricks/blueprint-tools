package glassbricks.factorio.blueprint.placement.poles

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.Vec2d
import glassbricks.factorio.blueprint.placement.CalculatedMapGraph
import glassbricks.factorio.blueprint.placement.DijkstrasResult
import glassbricks.factorio.blueprint.placement.dijkstras
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import io.github.oshai.kotlinlogging.KotlinLogging


public typealias CandidatePoleGraph = CalculatedMapGraph<PolePlacement>
public typealias DistanceMetric = (PolePlacement, PolePlacement) -> Double

private val logger = KotlinLogging.logger {}

public fun PolePlacements.getPoleGraph(distanceMetric: (PolePlacement, PolePlacement) -> Double): CandidatePoleGraph =
    CalculatedMapGraph(neighborsMap, distanceMetric)

public fun euclidianDistancePlus(amt: Double): DistanceMetric = { a, b ->
    a.position.distanceTo(b.position) + amt
}

public class DistanceBasedConnectivity(
    private val polePlacements: PolePlacements,
    private val poleGraph: CandidatePoleGraph,
    rootPoles: List<PolePlacement>
) {
    init {

        require(rootPoles.isNotEmpty()) {
            "Adding connectivity constraints without any root poles is the same as not adding any constraints"
        }
    }

    public val distances: DijkstrasResult<PolePlacement> = dijkstras(poleGraph, rootPoles)
    public fun addConstraints() {
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

    public companion object {
        private fun getRootPolesNear(
            poles: PolePlacements,
            graph: CandidatePoleGraph,
            relativePos: Vec2d
        ): List<PolePlacement> {
            val boundingBox = poles.model.placements.enclosingBox()
            val centerPoint = boundingBox.leftTop + boundingBox.size.emul(relativePos)
            return poles.model.placements.getPosInCircle(centerPoint, 8.0)
                .filter { it.prototype is ElectricPolePrototype }
                .toMutableList()
                .let {
                    @Suppress("UNCHECKED_CAST")
                    it as MutableList<PolePlacement>
                }
                .apply { sortBy { it.position.squaredDistanceTo(centerPoint) } }
                .let { getMaximalClique(graph, it) }
        }

        public fun fromExistingPoles(
            poles: PolePlacements,
            distanceMetric: (PolePlacement, PolePlacement) -> Double
        ): DistanceBasedConnectivity {
            @Suppress("UNCHECKED_CAST")
            val existingPoles =
                poles.model.placements.filter { it.isFixed && it.prototype is ElectricPolePrototype } as List<PolePlacement>
            val poleGraph = poles.getPoleGraph(distanceMetric)
            return DistanceBasedConnectivity(poles, poleGraph, existingPoles)
        }

        public fun fromAroundPt(
            poles: PolePlacements,
            relativePos: Vec2d,
            distanceMetric: (PolePlacement, PolePlacement) -> Double = euclidianDistancePlus(0.0)
        ): DistanceBasedConnectivity {
            val poleGraph = poles.getPoleGraph(distanceMetric)
            val rootPoles = getRootPolesNear(poles, poleGraph, relativePos)
            return DistanceBasedConnectivity(poles, poleGraph, rootPoles)
        }

        public fun fromExistingPolesOrPt(
            poles: PolePlacements,
            relativePos: Vec2d,
            distanceMetric: (PolePlacement, PolePlacement) -> Double = euclidianDistancePlus(0.0)
        ): DistanceBasedConnectivity {
            @Suppress("UNCHECKED_CAST")
            val existingPoles =
                poles.model.placements.filter { it.isFixed && it.prototype is ElectricPolePrototype } as List<PolePlacement>
            if (existingPoles.isEmpty()) {
                return fromAroundPt(poles, relativePos, distanceMetric)
            } else {
                val poleGraph = poles.getPoleGraph(distanceMetric)
                return DistanceBasedConnectivity(poles, poleGraph, existingPoles)
            }
        }
    }
}


private fun getMaximalClique(
    graph: CandidatePoleGraph,
    poles: Iterable<PolePlacement>,
) = buildList {
    for (pole in poles) {
        if (this.all { graph.hasEdge(it, pole) })
            this.add(pole)
    }
}

private fun Position.emul(other: Vec2d): Position = Position(x * other.x, y * other.y)
