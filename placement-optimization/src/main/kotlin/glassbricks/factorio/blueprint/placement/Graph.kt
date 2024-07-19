package glassbricks.factorio.blueprint.placement

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*


public class WeightedUnGraph<N>(public val nodes: List<N>) {
    public val size: Int get() = nodes.size

    private val nodeToIndex = nodes.withIndex().associateTo(HashMap()) { (index, node) -> node to index }
    public fun indexOf(node: N): Int = nodeToIndex[node] ?: -1

    public val adj: Array<ArrayList<ToEdge>> = Array(nodes.size) { ArrayList<ToEdge>() }

    public fun addEdge(from: N, to: N, weight: Double): Boolean {
        val fromIndex = nodeToIndex[from] ?: return false
        val toIndex = nodeToIndex[to] ?: return false
        adj[fromIndex].add(ToEdge(toIndex, weight))
        adj[toIndex].add(ToEdge(fromIndex, weight))
        return true
    }

    public fun hasEdge(from: N, to: N): Boolean {
        val fromIndex = nodeToIndex[from] ?: return false
        val toIndex = nodeToIndex[to] ?: return false
        return adj[fromIndex].any { it.nodeIndex == toIndex }
    }

    public fun neighborsOf(node: N): List<ToEdge> = adj[nodes.indexOf(node)]

    public data class ToEdge(public val nodeIndex: Int, public val weight: Double)
}

public class DijkstrasResult<N>(
    public val distancesArr: DoubleArray,
    public val distancesMap: Map<N, Double>,
//    public val predecessors: Map<N, N>
)

private data class QueueEntry(val nodeIndex: Int, val distance: Double) : Comparable<QueueEntry> {
    override fun compareTo(other: QueueEntry): Int = distance.compareTo(other.distance)
}

private val logger = KotlinLogging.logger {}

public fun <N> dijkstras(
    graph: WeightedUnGraph<N>,
    startNodes: Iterable<N>
): DijkstrasResult<N> {
    val dist = DoubleArray(graph.size) { Double.POSITIVE_INFINITY }
    fun N.index(): Int = graph.indexOf(this)
    val seen = BooleanArray(graph.size)
//    val pred = HashMap<N, N>()
    val queue = PriorityQueue<QueueEntry>()

    for (node in startNodes) {
        val index = node.index()
        dist[index] = 0.0
        queue.add(QueueEntry(index, 0.0))
    }

    while (queue.isNotEmpty()) {
        val (curNode, curDist) = queue.remove()
        if (seen[curNode]) continue
        seen[curNode] = true
        for ((neighbor, weight) in graph.adj[curNode]) {
            val newDist = curDist + weight
            if (newDist < dist[neighbor]) {
                dist[neighbor] = newDist
//                pred[graph.nodes[neighbor]] = graph.nodes[curNode]
                queue.add(QueueEntry(neighbor, newDist))
            }
        }
    }
    return DijkstrasResult(
        dist,
        buildMap {
            graph.nodes.forEachIndexed { index, node -> put(node, dist[index]) }
        }
    )
}
