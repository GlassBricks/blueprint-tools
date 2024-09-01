package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.ops.ItemTransportGraph
import glassbricks.factorio.blueprint.placement.ops.LogisticsEdgeType
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
        fun oppositeNotIsolated(): Underground?
        fun opposite(isolated: Boolean): Underground
    }

    data class Belt(override val prototype: TransportBeltPrototype) : BeltType {
        override fun toString(): String = "<${prototype.name}>"
        override val hasInput: Boolean get() = true
        override val hasOutput: Boolean get() = true
    }

    data class InputUnderground(
        override val prototype: UndergroundBeltPrototype,
        override val isIsolated: Boolean = false,
    ) : Underground {
        override fun toString(): String = "<${prototype.name}_in${if (isIsolated) "_isolated" else ""}>"
        override val hasInput: Boolean get() = true
        override val hasOutput: Boolean get() = false

        override fun oppositeNotIsolated(): OutputUnderground? = if (!isIsolated) OutputUnderground(prototype) else null
        override fun opposite(isolated: Boolean): OutputUnderground = OutputUnderground(prototype, isolated)
    }

    data class OutputUnderground(
        override val prototype: UndergroundBeltPrototype,
        override val isIsolated: Boolean = false,
    ) :
        Underground {
        override fun toString(): String = "<${prototype.name}_out${if (isIsolated) "_isolated" else ""}>"
        override val hasInput: Boolean get() = false
        override val hasOutput: Boolean get() = true

        override fun oppositeNotIsolated(): InputUnderground? = if (!isIsolated) InputUnderground(prototype) else null
        override fun opposite(isolated: Boolean): InputUnderground = InputUnderground(prototype, isolated)
    }

}

fun ItemTransportGraph.Node.isIsolatedUnderground(): Boolean {
    if (entity !is UndergroundBelt) return false
    return when (entity.ioType) {
        IOType.Input -> outEdges.none { it.type == LogisticsEdgeType.Belt && it.to.entity is UndergroundBelt }
        IOType.Output -> inEdges.none { it.type == LogisticsEdgeType.Belt && it.from.entity is UndergroundBelt }
    }
}

fun ItemTransportGraph.Node.getBeltType(): BeltType? = when (val prototype = prototype) {
    is TransportBeltPrototype -> BeltType.Belt(prototype)
    is UndergroundBeltPrototype -> when ((entity as UndergroundBelt).ioType) {
        IOType.Input -> BeltType.InputUnderground(
            prototype,
            isIsolated = outEdges.none { it.type == LogisticsEdgeType.Belt && it.to.entity is UndergroundBelt })

        IOType.Output -> BeltType.OutputUnderground(
            prototype,
            isIsolated = inEdges.none { it.type == LogisticsEdgeType.Belt && it.from.entity is UndergroundBelt })
    }

    else -> null
}
