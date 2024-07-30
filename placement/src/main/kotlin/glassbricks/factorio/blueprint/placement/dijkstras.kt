package glassbricks.factorio.blueprint.placement

import java.util.*


class DijkstrasResult<N>(
    val distances: Map<N, Double>,
)

private data class QueueEntry<N>(val node: N, val distance: Double) : Comparable<QueueEntry<N>> {
    override fun compareTo(other: QueueEntry<N>): Int = distance.compareTo(other.distance)
}

fun <N> dijkstras(
    graph: GraphLike<N>,
    startNodes: Iterable<N>
): DijkstrasResult<N> {
    val dist = hashMapOf<N, Double>()
    val seen = hashSetOf<N>()
    val queue = PriorityQueue<QueueEntry<N>>()

    for (node in startNodes) {
        dist[node] = 0.0
        queue.add(QueueEntry(node, 0.0))
    }

    while (queue.isNotEmpty()) {
        val (curNode, curDist) = queue.remove()
        if (curNode in seen) continue
        seen.add(curNode)
        for (neighbor in graph.neighborsOf(curNode)) {
            val newDist = curDist + graph.edgeWeight(curNode, neighbor)
            val oldDist = dist[neighbor]
            if (oldDist == null || newDist < oldDist) {
                dist[neighbor] = newDist
                queue.add(QueueEntry(neighbor, newDist))
            }
        }
    }
    return DijkstrasResult(dist)
}
