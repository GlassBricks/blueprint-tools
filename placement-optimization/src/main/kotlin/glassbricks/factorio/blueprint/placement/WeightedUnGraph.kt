package glassbricks.factorio.blueprint.placement

import java.util.ArrayList
import java.util.HashMap

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
