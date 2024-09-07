package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.MultiTable
import glassbricks.factorio.blueprint.placement.add
import glassbricks.factorio.blueprint.placement.mutableMultiTableOf
import glassbricks.factorio.blueprint.placement.retainAllM

typealias BeltLineId = Int

interface BeltConfig {
    val pos: TilePosition
    fun getOptions(): MultiTable<CardinalDirection, BeltType, BeltLineId>
    val canBeEmpty: Boolean
    val propagatesForward: Boolean
    val propagatesBackward: Boolean
    val mustNotOutputIn: CardinalDirection?
}

interface MutableBeltConfig : BeltConfig {
    fun addOption(direction: CardinalDirection, beltType: BeltType, lineId: BeltLineId)
    fun makeLineStart(direction: CardinalDirection, lineId: BeltLineId)
    fun makeLineEnd(direction: CardinalDirection, lineId: BeltLineId)

    fun forceIsId(lineId: BeltLineId)

    // will also add as option
    fun forceAs(direction: CardinalDirection, lineId: BeltLineId, beltType: BeltType)

    fun mustNotOutputIn(direction: CardinalDirection)
}


internal class BeltConfigImpl(
    override val pos: TilePosition,
) : BeltConfig, MutableBeltConfig {
    private val beltOptions =
        mutableMultiTableOf<CardinalDirection, BeltType, BeltLineId>()

    private var isLineStart: Boolean = false
    private var isLineEnd: Boolean = false
    override val propagatesForward: Boolean get() = !isLineEnd
    override val propagatesBackward: Boolean get() = !isLineStart
    override var mustNotOutputIn: CardinalDirection? = null

    private var forcedDirection: CardinalDirection? = null
        set(value) {
            check(field == null || field == value) { "Direction already set" }
            field = value
        }
    private var forcedBeltId: BeltLineId? = null
        set(value) {
            check(field == null || field == value) { "Belt ID already set" }
            field = value
        }
    private var forcedBeltType: BeltType? = null
        set(value) {
            check(field == null || field == value) { "Belt type already set" }
            field = value
        }


    override fun addOption(
        direction: CardinalDirection,
        beltType: BeltType,
        lineId: BeltLineId,
    ) {
        require(lineId != 0) { "Line ID must be non-zero" }
        beltOptions.add(direction, beltType, lineId)
    }

    override fun makeLineStart(direction: CardinalDirection, lineId: BeltLineId) {
        isLineStart = true
        forcedDirection = direction
        forcedBeltId = lineId
    }

    override fun makeLineEnd(
        direction: CardinalDirection,
        lineId: BeltLineId,
    ) {
        isLineEnd = true
        forcedDirection = direction
        forcedBeltId = lineId
    }

    override fun forceIsId(lineId: BeltLineId) {
        forcedBeltId = lineId
    }

    override fun forceAs(
        direction: CardinalDirection,
        lineId: BeltLineId,
        beltType: BeltType,
    ) {
        forcedDirection = direction
        forcedBeltId = lineId
        forcedBeltType = beltType
        addOption(direction, beltType, lineId)
    }

    override fun mustNotOutputIn(direction: CardinalDirection) {
        mustNotOutputIn = direction
    }

    override val canBeEmpty: Boolean
        get() = forcedBeltId == null && forcedBeltType == null

    override fun getOptions(): MultiTable<CardinalDirection, BeltType, BeltLineId> {
        if (forcedBeltId == null && forcedDirection == null && forcedBeltType == null) return beltOptions
        return beltOptions.toMutableMap().apply {
            retainAllM { direction, beltType, lineId ->
                (forcedDirection == null || direction == forcedDirection) &&
                        (forcedBeltType == null || beltType == forcedBeltType) &&
                        (forcedBeltId == null || lineId == forcedBeltId)
            }
        }
    }
}
