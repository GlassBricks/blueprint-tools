package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.belt.BeltType.OutputUnderground

typealias BeltLineId = Int

interface BeltTile {
    val pos: TilePosition
    val canBeEmpty: Boolean
    val propagatesForward: Boolean
    val propagatesBackward: Boolean
    val forcedId: BeltLineId?

    val options: Set<BeltOption>
}

data class BeltOption(
    val direction: CardinalDirection,
    val beltType: BeltType,
    val lineId: BeltLineId,
)


class BeltConfig(override val pos: TilePosition) : BeltTile {
    private val beltOptions = mutableSetOf<BeltOption>()

    private var isLineStart: Boolean = false
    private var isLineEnd: Boolean = false
    override val propagatesForward: Boolean get() = !isLineEnd
    override val propagatesBackward: Boolean get() = !isLineStart
    private val noInputDirections = mutableSetOf<CardinalDirection>()

    private var cachedOptions: Set<BeltOption>? = beltOptions

    private var forcedDirection: CardinalDirection? = null
        set(value) {
            check(field == null || field == value) { "Direction already set" }
            field = value
            cachedOptions = null
        }
    override var forcedId: BeltLineId? = null
        private set(value) {
            check(field == null || field == value) { "Belt ID already set" }
            field = value
            cachedOptions = null
        }
    private var forcedBeltType: BeltType? = null
        set(value) {
            check(field == null || field == value) { "Belt type already set" }
            field = value
            cachedOptions = null
        }

    fun mustNotTakeInputIn(direction: CardinalDirection) {
        noInputDirections.add(direction)
        cachedOptions = null
    }

    fun addOption(
        direction: CardinalDirection,
        beltType: BeltType,
        lineId: BeltLineId,
    ) {
        require(lineId != 0) { "Line ID must be non-zero" }
        beltOptions.add(BeltOption(direction, beltType, lineId))
    }

    fun makeLineStart(direction: CardinalDirection, lineId: BeltLineId) {
        isLineStart = true
        forcedDirection = direction
        forcedId = lineId
    }

    fun makeLineEnd(
        direction: CardinalDirection,
        lineId: BeltLineId,
    ) {
        isLineEnd = true
        forcedDirection = direction
        forcedId = lineId
    }

    fun forceIsId(lineId: BeltLineId) {
        forcedId = lineId
    }

    fun forceAs(
        direction: CardinalDirection,
        lineId: BeltLineId,
        beltType: BeltType,
    ) {
        forcedDirection = direction
        forcedId = lineId
        forcedBeltType = beltType
        addOption(direction, beltType, lineId)
    }

    override val canBeEmpty: Boolean get() = forcedId == null && forcedBeltType == null

    override val options get() = cachedOptions ?: computeBeltOptions().also { cachedOptions = it }

    private fun computeBeltOptions(): MutableSet<BeltOption> = beltOptions.filterTo(mutableSetOf()) {
        if (forcedDirection != null && it.direction != forcedDirection) return@filterTo false
        if (forcedBeltType != null && it.beltType != forcedBeltType) return@filterTo false
        if (forcedId != null && it.lineId != forcedId) return@filterTo false
        for (inDirection in noInputDirections) {
            val allowed =
                it.direction == inDirection.oppositeDir() || (it.direction == inDirection && it.beltType is OutputUnderground)
            if (!allowed) return@filterTo false
        }
        true
    }
}
