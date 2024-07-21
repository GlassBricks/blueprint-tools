package glassbricks.factorio.blueprint.placement


public interface GraphLike<T> {
    public val nodes: Collection<T>

    public fun hasEdge(from: T, to: T): Boolean

    public fun edgeWeight(from: T, to: T): Double
    public fun neighborsOf(node: T): Iterable<T>
}

public class CalculatedMapGraph<T>(
    private val neighborMap: Map<T, List<T>>,
    private val getWeight: (T, T) -> Double
) : GraphLike<T> {
    public override val nodes: Set<T> = neighborMap.keys

    public override fun hasEdge(from: T, to: T): Boolean = neighborMap[from]?.contains(to) ?: false
    public override fun neighborsOf(node: T): List<T> = neighborMap[node].orEmpty()

    override fun edgeWeight(from: T, to: T): Double = getWeight(from, to)
}
