package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.MultiMap
import glassbricks.factorio.blueprint.placement.MutableMultiMap
import java.util.EnumMap

typealias BeltLineId = Int

interface BeltConfig {
    fun getOptions(): Map<CardinalDirection, MultiMap<BeltType, BeltLineId>>
    val canBeEmpty: Boolean
    val propagatesForward: Boolean
    val propagatesBackward: Boolean
}

interface MutableBeltConfig : BeltConfig {
    fun addOption(direction: CardinalDirection, beltType: BeltType, lineId: BeltLineId)
    fun makeLineStart(direction: CardinalDirection, lineId: BeltLineId)
    fun makeLineEnd(direction: CardinalDirection, lineId: BeltLineId)

    fun forceNonEmpty(direction: CardinalDirection, lineId: BeltLineId)
    fun forceAs(direction: CardinalDirection, lineId: BeltLineId, beltType: BeltType)
}


internal class BeltConfigImpl : BeltConfig, MutableBeltConfig {
    private val beltOptions =
        EnumMap<CardinalDirection, MutableMultiMap<BeltType, BeltLineId>>(CardinalDirection::class.java)

    private var isLineStart: Boolean = false
    private var isLineEnd: Boolean = false
    override val propagatesForward: Boolean get() = !isLineEnd
    override val propagatesBackward: Boolean get() = !isLineStart

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
        beltOptions.getOrPut(direction) { MultiMap() }.add(beltType, lineId)
    }

    override fun makeLineStart(direction: CardinalDirection, lineId: BeltLineId) {
        isLineStart = true
        forceNonEmpty(direction, lineId)
    }

    override fun makeLineEnd(
        direction: CardinalDirection,
        lineId: BeltLineId,
    ) {
        isLineEnd = true
        forceNonEmpty(direction, lineId)
    }

    override fun forceNonEmpty(
        direction: CardinalDirection,
        lineId: BeltLineId,
    ) {
        forcedDirection = direction
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
    }

    override val canBeEmpty: Boolean
        get() = forcedBeltId == null && forcedDirection == null && forcedBeltType == null

    override fun getOptions(): Map<CardinalDirection, MultiMap<BeltType, BeltLineId>> {
        if (forcedBeltId == null && forcedDirection == null && forcedBeltType == null) return beltOptions
        return beltOptions.filterKeys { forcedDirection == null || it == forcedDirection }
            .mapValues {
                it.value.filterKeys {
                    forcedBeltType == null || it == forcedBeltType
                }.mapValues {
                    it.value.filterTo(hashSetOf()) {
                        forcedBeltId == null || it == forcedBeltId
                    }
                }
            }
    }
}
