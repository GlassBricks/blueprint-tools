package glassbricks.factorio.blueprint.placement.grid

import com.google.ortools.sat.CpModel
import com.google.ortools.sat.IntVar
import com.google.ortools.util.Domain
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.MultiMap
import glassbricks.factorio.blueprint.placement.asLit
import glassbricks.factorio.blueprint.placement.enumMapOf
import glassbricks.factorio.blueprint.placement.mapToArray
import glassbricks.factorio.blueprint.placement.mapValuesNotNull
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype

interface CellVars {
    val belt: CellBeltVars
}

internal class CellVarsImpl(
    cp: CpModel,
    config: CellConfig,
) : CellVars, CellBeltVars {
    override val belt: CellBeltVars get() = this
    override val isEmpty = cp.newBoolVar("isEmpty").asLit()

    override val lineId: IntVar
    override val lineIdDomainMap: Map<Int, Literal>
    private val beltOptions = config.belt.getOptions()
    override fun getOptions(): Map<CardinalDirection, MultiMap<BeltType, BeltLineId>> = beltOptions

    override val canBeEmpty: Boolean = config.belt.canBeEmpty
    override val propagatesIdForward: Boolean = config.belt.propagatesIdForward
    override val propagatesIdBackward: Boolean = config.belt.propagatesIdBackward

    init {
        if (!canBeEmpty) cp.addEquality(isEmpty, 0)
    }

    init {
        val possibleBeltIds = hashSetOf<Long>()
        for (a in beltOptions.values) for (b in a.values) for (c in b) {
            possibleBeltIds.add(c.toLong())
        }
        this.lineId = cp.newIntVarFromDomain(Domain.fromValues(possibleBeltIds.toLongArray()), "beltLineId")

        val beltIdDomainMap = mutableMapOf<Int, Literal>()
        for (beltId in possibleBeltIds) {
            val lit = if (beltId == -1L) isEmpty else cp.newBoolVar("beltLineId_$beltId").asLit()
            cp.addEquality(lineId, beltId.toLong()).onlyEnforceIf(lit)
            cp.addDifferent(lineId, beltId.toLong()).onlyEnforceIf(!lit)
            beltIdDomainMap[beltId.toInt()] = lit.asLit()
        }
        this.lineIdDomainMap = beltIdDomainMap
    }

    override val selectVars: BeltSelectVars = beltOptions.mapValues { (direction, beltTypes) ->
        beltTypes.mapValues { (beltType, ids) ->
            val selectedVar = cp.newBoolVar("selected_${beltType}_${direction}").asLit()
            cp.addBoolOr(ids.map { lineIdDomainMap[it]!! })
                .onlyEnforceIf(selectedVar)
            selectedVar
        }
    }

    init {
        cp.addExactlyOne(
            selectVars.values.flatMap { it.values } + isEmpty
        )
    }

    override val hasOutputIn: Map<CardinalDirection, Literal> =
        this.selectVars.mapValuesNotNull { (direction, beltTypeSelected) ->
            val withOutput = beltTypeSelected.keys.filter { it.hasOutput }
            if (withOutput.isEmpty()) return@mapValuesNotNull null

            val hasOutVar = cp.newBoolVar("hasOutputIn_${direction}").asLit()
            cp.addBoolOr(withOutput.mapToArray { beltTypeSelected[it]!! })
                .onlyEnforceIf(hasOutVar)
            hasOutVar
        }

    private val ugConnectors_ = enumMapOf<CardinalDirection, MutableMap<UndergroundBeltPrototype, Literal>>()
    override val ugConnectors: UgConnectorVars get() = ugConnectors_
    internal fun ensureUgConnector(
        cp: CpModel,
        direction: CardinalDirection,
        prototype: UndergroundBeltPrototype,
    ): Literal {
        val map = ugConnectors_.getOrPut(direction) { hashMapOf() }
        return map.getOrPut(prototype) {
            cp.newBoolVar("ugConnector_${direction}_${prototype.name}").asLit()
        }
    }
}
