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
    override val propagatesForward: Boolean = config.belt.propagatesForward
    override val propagatesBackward: Boolean = config.belt.propagatesBackward

    init {
        if (!canBeEmpty) cp.addEquality(isEmpty, 0)
    }

    init {
        val possibleBeltIds = hashSetOf<Long>()
        possibleBeltIds.add(0)
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

    override val selectedBelt: BeltSelectVars = beltOptions.mapValues { (direction, beltTypes) ->
        beltTypes.mapValues { (beltType, ids) ->
            val selectedVar = cp.newBoolVar("selected_${beltType}_${direction}").asLit()
            cp.addBoolOr(ids.map { lineIdDomainMap[it]!! })
                .onlyEnforceIf(selectedVar)
            selectedVar
        }
    }

    init {
        cp.addExactlyOne(
            selectedBelt.values.flatMap { it.values } + isEmpty
        )
    }

    override val hasOutputIn: Map<CardinalDirection, Literal> =
        this.selectedBelt.mapValuesNotNull { (direction, beltTypeSelected) ->
            val withOutput = beltTypeSelected.keys.filter { it.hasOutput }
            if (withOutput.isEmpty()) return@mapValuesNotNull null

            val hasOutVar = cp.newBoolVar("hasOutputIn_${direction}").asLit()
            val selected = withOutput.map { beltTypeSelected[it]!! }
            // has out <=> any selected
            cp.addBoolOr(selected).onlyEnforceIf(hasOutVar)
            for (literal in selected) cp.addImplication(literal, hasOutVar)
            hasOutVar
        }

    // todo: handle sideloading, which makes hasInputIn more complicated
    override val hasInputIn: Map<CardinalDirection, Literal> =
        this.selectedBelt.mapValuesNotNull { (direction, beltTypeSelected) ->
            val withInput = beltTypeSelected.keys.filter { it.hasInput }
            if (withInput.isEmpty()) return@mapValuesNotNull null

            val hasInVar = cp.newBoolVar("hasInputIn_${direction}").asLit()
            cp.addBoolOr(withInput.mapToArray { beltTypeSelected[it]!! })
                .onlyEnforceIf(hasInVar)
            hasInVar
        }

    init {
        // this is redundant, but it might help cp
        cp.addAtMostOne(hasOutputIn.values)
    }

    private val ugConnectorSelected_ = enumMapOf<CardinalDirection, MutableMap<UndergroundBeltPrototype, Literal>>()
    override val ugConnectorSelected: UgConnectorVars get() = ugConnectorSelected_
    private val ugConnectorId_ = mutableMapOf<Axis, MutableMap<UndergroundBeltPrototype, IntVar>>()
    override val ugConnectorId: UgConnectorIds get() = ugConnectorId_
    internal fun ensureUgConnector(
        cp: CpModel,
        direction: CardinalDirection,
        prototype: UndergroundBeltPrototype,
    ): Literal {
        val map = ugConnectorSelected_.getOrPut(direction) { hashMapOf() }
        return map.getOrPut(prototype) {
            cp.newBoolVar("ugConnector_${direction}_${prototype.name}")
                .asLit()
        }
    }

    internal fun constrainUgId(cp: CpModel) {
        for ((direction, map) in ugConnectorSelected_) {
            val axis = direction.axis
            val axisMap = ugConnectorId_.getOrPut(axis) { hashMapOf() }
            for ((prototype, connectorSelected) in map) {
                val ugId = axisMap.getOrPut(prototype) {
                    cp.newIntVar(0, Int.MAX_VALUE.toLong(), "ugConnectorId_${axis}_${prototype.name}")
                }
                cp.addDifferent(ugId, 0).onlyEnforceIf(connectorSelected)
                val oppositeDir = direction.oppositeDir()
                if (oppositeDir.ordinal > direction.ordinal) {
                    val oppositeSelected = ugConnectorSelected_[oppositeDir]?.get(prototype)
                    if (oppositeSelected != null) cp.addAtMostOne(listOf(connectorSelected, oppositeSelected))
                }
            }
        }
    }
}
