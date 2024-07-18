package glassbricks.factorio.blueprint.poleopt

import java.util.*


public class WeightedGraph<N> {
    private val nodes: MutableMap<N, MutableMap<N, Double>> = mutableMapOf()

    public fun addNode(node: N): Boolean {
        if (nodes.containsKey(node)) return false
        nodes[node] = mutableMapOf()
        return true
    }

    /**
     * Also adds an edge if it doesn't exist
     */
    public fun setWeight(from: N, to: N, weight: Double) {
        nodes[from]?.set(to, weight)
        nodes[to]?.set(from, weight)
    }

    public fun addEdge(from: N, to: N, weight: Double): Boolean {
        if (hasEdge(from, to)) return false
        setWeight(from, to, weight)
        return true
    }

    public fun getWeight(from: N, to: N): Double? = nodes[from]?.get(to)

    public fun hasEdge(from: N, to: N): Boolean = nodes[from]?.containsKey(to) ?: false

    public fun neighborsOf(node: N): Set<N> = nodes[node]?.keys ?: emptySet()
    public fun neighborsWithWeightsOf(node: N): Map<N, Double> = nodes[node] ?: emptyMap()

    public fun nodes(): Set<N> = nodes.keys
}

public class DijkstrasResult<N>(
    public val distances: Map<N, Double>,
    public val predecessors: Map<N, N>
)

private class QueueEntry<N>(val node: N, val distance: Double) : Comparable<QueueEntry<N>> {
    override fun compareTo(other: QueueEntry<N>): Int = distance.compareTo(other.distance)
}

public fun <N> dijkstras(
    graph: WeightedGraph<N>,
    startNodes: Iterable<N>
): DijkstrasResult<N> {
    val dist = mutableMapOf<N, Double>()
    val seen = mutableSetOf<N>()
    val pred = mutableMapOf<N, N>()
    val queue = PriorityQueue<QueueEntry<N>>()

    for (node in startNodes) {
        dist[node] = 0.0
        queue.add(QueueEntry(node, 0.0))
    }

    while (queue.isNotEmpty()) {
        val current = queue.remove().node
        if (current in seen) continue
        seen.add(current)
        val curDist = dist[current]!!
        for ((neighbor, weight) in graph.neighborsWithWeightsOf(current)) {
            val newDist = curDist + weight
            if (newDist < dist.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                dist[neighbor] = newDist
                pred[neighbor] = current
                queue.add(QueueEntry(neighbor, newDist))
            }
        }
    }

    return DijkstrasResult(dist, pred)
}
