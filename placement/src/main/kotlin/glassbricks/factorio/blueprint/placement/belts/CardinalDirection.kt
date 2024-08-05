package glassbricks.factorio.blueprint.placement.belts

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.belts.CardinalDirection.*


enum class CardinalDirection {
    North,
    East,
    South,
    West;

    fun oppositeDir(): CardinalDirection = when (this) {
        North -> South
        East -> West
        South -> North
        West -> East
    }

    val dx: Int
        get() = when (this) {
            North -> 0
            East -> 1
            South -> 0
            West -> -1
        }
    val dy: Int
        get() = when (this) {
            North -> -1
            East -> 0
            South -> 1
            West -> 0
        }
}

fun TilePosition.shifted(direction: CardinalDirection, amt: Int = 1): TilePosition = when (direction) {
    North -> add(0, -amt)
    East -> add(amt, 0)
    South -> add(0, amt)
    West -> add(-amt, 0)
}

@JvmInline
value class PerDirection<out T>(val values: Array<out T>) : Collection<T> {
    init {
        require(values.size == 4) { "PerDirection must be initialized with 4 values" }
    }

    operator fun get(direction: CardinalDirection): T = values[direction.ordinal]
    override operator fun iterator(): Iterator<T> = values.iterator()

    override val size: Int get() = 4
    override fun isEmpty(): Boolean = false

    override fun contains(element: @UnsafeVariance T): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<@UnsafeVariance T>): Boolean = elements.all { values.contains(it) }
}

inline fun <T> PerDirection(init: (CardinalDirection) -> T): PerDirection<T> {
    val values = Array<Any?>(4) { init(entries[it]) }
    @Suppress("UNCHECKED_CAST")
    return PerDirection(values as Array<T>)
}

typealias PerDirectionVars = PerDirection<Literal>
