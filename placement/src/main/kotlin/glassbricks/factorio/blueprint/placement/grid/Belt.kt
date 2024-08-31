package glassbricks.factorio.blueprint.placement.grid

import com.google.ortools.sat.IntVar
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.MultiMap
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype

typealias BeltLineId = Int

sealed interface BeltType {
    override fun toString(): String
    val hasOutput: Boolean
    val hasInput: Boolean

    sealed interface Underground : BeltType {
        val prototype: UndergroundBeltPrototype
        val isIsolated: Boolean
        fun opposite(): Underground?
    }

    data class Belt(val prototype: TransportBeltPrototype) : BeltType {
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

interface CellBeltConfig {
    fun getOptions(): Map<CardinalDirection, MultiMap<BeltType, BeltLineId>>
    val canBeEmpty: Boolean
    val propagatesForward: Boolean
    val propagatesBackward: Boolean
}

interface MutableCellBeltConfig : CellBeltConfig {
    fun addOption(direction: CardinalDirection, beltType: BeltType, lineId: BeltLineId)
    fun makeLineStart(direction: CardinalDirection, lineId: BeltLineId)
    fun makeLineEnd(direction: CardinalDirection, lineId: BeltLineId)

    fun forceNonEmpty(direction: CardinalDirection, lineId: BeltLineId)
    fun forceAs(direction: CardinalDirection, lineId: BeltLineId, beltType: BeltType)
}

typealias BeltSelectVars = Map<CardinalDirection, Map<BeltType, Literal>>
typealias UgConnectorVars = Map<CardinalDirection, Map<UndergroundBeltPrototype, Literal>>
typealias UgConnectorIds = Map<Axis, Map<UndergroundBeltPrototype, IntVar>>

interface CellBeltVars : CellBeltConfig {
    val isEmpty: Literal
    val lineId: IntVar
    val lineIdDomainMap: Map<Int, Literal>
    val selectedBelt: BeltSelectVars
    val hasOutputIn: Map<CardinalDirection, Literal>
    val hasInputIn: Map<CardinalDirection, Literal>
    val ugConnectorSelected: UgConnectorVars
    val ugConnectorId: UgConnectorIds
}
