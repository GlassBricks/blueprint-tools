package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.TransportBelt
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.TransportBeltConnectablePrototype
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype


data class BeltTier(
    val beltProto: TransportBeltPrototype,
    val ugProto: UndergroundBeltPrototype,
) {
    val belt: BeltType.Belt = BeltType.Belt(beltProto)
    val inputUg: BeltType.InputUnderground = BeltType.InputUnderground(ugProto)
    val outputUg: BeltType.OutputUnderground = BeltType.OutputUnderground(ugProto)
}

fun BlueprintPrototypes.getBeltTier(belt: TransportBeltPrototype): BeltTier? {
    val ugId = belt.related_underground_belt ?: return null
    val ug = this[ugId] as? UndergroundBeltPrototype ?: return null
    return BeltTier(belt, ug)
}

fun BlueprintPrototypes.getBeltTier(beltName: String): BeltTier? =
    this.dataRaw.`transport-belt`[beltName]?.let { getBeltTier(it) }

fun BlueprintPrototypes.getAllBeltTiers(): Map<TransportBeltConnectablePrototype, BeltTier> {
    val beltTiers = dataRaw.`transport-belt`.values.mapNotNull { getBeltTier(it) }
    return buildMap {
        for (tier in beltTiers) {
            put(tier.beltProto, tier)
            put(tier.ugProto, tier)
        }
    }
}

sealed interface BeltType {
    override fun toString(): String
    val hasOutput: Boolean
    val hasInput: Boolean
    val prototype: TransportBeltConnectablePrototype

    sealed interface Underground : BeltType {
        override val prototype: UndergroundBeltPrototype
        val isIsolated: Boolean
        fun opposite(): Underground?
    }

    data class Belt(override val prototype: TransportBeltPrototype) : BeltType {
        override fun toString(): String = prototype.name
        override val hasInput: Boolean get() = true
        override val hasOutput: Boolean get() = true
    }

    data class InputUnderground(
        override val prototype: UndergroundBeltPrototype,
        override val isIsolated: Boolean = false,
    ) : Underground {
        override fun toString(): String = "${prototype.name}_in${if (isIsolated) "_isolated" else ""}"
        override val hasInput: Boolean get() = true
        override val hasOutput: Boolean get() = false

        override fun opposite(): OutputUnderground? = if (!isIsolated) OutputUnderground(prototype) else null
    }

    data class OutputUnderground(
        override val prototype: UndergroundBeltPrototype,
        override val isIsolated: Boolean = false,
    ) :
        Underground {
        override fun toString(): String = "${prototype.name}_out${if (isIsolated) "_isolated" else ""}"
        override val hasInput: Boolean get() = false
        override val hasOutput: Boolean get() = true

        override fun opposite(): InputUnderground? = if (!isIsolated) InputUnderground(prototype) else null
    }

}

fun BlueprintEntity.getBeltType(): BeltType? = when (this) {
    is TransportBelt -> BeltType.Belt(this.prototype)
    is UndergroundBelt -> when (this.ioType) {
        IOType.Input -> BeltType.InputUnderground(this.prototype)
        IOType.Output -> BeltType.OutputUnderground(this.prototype)
    }

    else -> null
}
