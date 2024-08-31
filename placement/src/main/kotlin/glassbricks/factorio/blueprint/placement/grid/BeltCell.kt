package glassbricks.factorio.blueprint.placement.grid

import com.google.ortools.sat.IntVar
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.MultiMap
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype

typealias BeltLineId = Int

sealed class BeltType {
    abstract override fun toString(): String
    abstract val hasOutput: Boolean

    data class Belt(val prototype: TransportBeltPrototype) : BeltType() {
        override fun toString(): String = prototype.name
        override val hasOutput: Boolean get() = true
    }

    data class InputUnderground(val prototype: UndergroundBeltPrototype) : BeltType() {
        override fun toString(): String = "${prototype.name}_in"
        override val hasOutput: Boolean get() = false
    }

    data class OutputUnderground(val prototype: UndergroundBeltPrototype) : BeltType() {
        override fun toString(): String = "${prototype.name}_out"
        override val hasOutput: Boolean get() = true
    }
}

interface CellBeltConfig {
    fun getOptions(): Map<CardinalDirection, MultiMap<BeltType, BeltLineId>>
    val canBeEmpty: Boolean
    val propagatesIdForward: Boolean
    val propagatesIdBackward: Boolean
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

interface CellBeltVars : CellBeltConfig {
    val isEmpty: Literal
    val lineId: IntVar
    val lineIdDomainMap: Map<Int, Literal>
    val selectVars: BeltSelectVars
    val hasOutputIn: Map<CardinalDirection, Literal>
    val ugConnectors: UgConnectorVars
}
