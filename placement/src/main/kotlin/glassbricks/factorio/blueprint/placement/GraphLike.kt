package glassbricks.factorio.blueprint.placement


interface GraphLike<T> {
    val nodes: Collection<T>

    fun hasEdge(from: T, to: T): Boolean

    fun edgeWeight(from: T, to: T): Double
    fun neighborsOf(node: T): Iterable<T>
}

class CalculatedMapGraph<T>(
    private val neighborMap: Map<T, List<T>>,
    private val getWeight: (T, T) -> Double
) : GraphLike<T> {
    override val nodes: Set<T> = neighborMap.keys

    override fun hasEdge(from: T, to: T): Boolean = neighborMap[from]?.contains(to) ?: false
    override fun neighborsOf(node: T): List<T> = neighborMap[node].orEmpty()

    override fun edgeWeight(from: T, to: T): Double = getWeight(from, to)
}
