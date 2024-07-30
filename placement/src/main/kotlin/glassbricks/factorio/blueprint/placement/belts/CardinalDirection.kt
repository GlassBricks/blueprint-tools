package glassbricks.factorio.blueprint.placement.belts

import glassbricks.factorio.blueprint.placement.Literal


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

    val vec get() = AsVec(this)

    companion object {
        fun fromOrdinal(ordinal: Int): CardinalDirection = entries[ordinal]
    }
}

@JvmInline
value class AsVec(private val direction: CardinalDirection) {
    operator fun component1(): Int = direction.dx
    operator fun component2(): Int = direction.dy
}

@JvmInline
value class PerDirection<out T>(val values: Array<out T>) : Iterable<T> {
    init {
        require(values.size == 4) { "PerDirection must be initialized with 4 values" }
    }

    operator fun get(direction: CardinalDirection): T = values[direction.ordinal]
    override operator fun iterator(): Iterator<T> = values.iterator()
}

typealias PerDirectionVars = PerDirection<Literal>
