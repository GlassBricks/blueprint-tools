package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpModel
import com.google.ortools.sat.IntVar
import com.google.ortools.sat.Literal
import com.google.ortools.util.Domain
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.MultiMap
import glassbricks.factorio.blueprint.placement.mapToArray
import glassbricks.factorio.blueprint.placement.mapValuesNotNull

typealias BeltSelectVars = Map<CardinalDirection, Map<BeltType, Literal>>

interface Belt : BeltConfig {
    val isEmpty: Literal
    val lineId: IntVar
    val lineIdDomainMap: Map<Int, Literal>
    val selectedBelt: BeltSelectVars
    val hasOutputIn: Map<CardinalDirection, Literal>
    val hasInputIn: Map<CardinalDirection, Literal>
}


internal class BeltImpl(
    cp: CpModel,
    config: BeltConfig,
) : Belt {
    override val isEmpty: Literal = cp.newBoolVar("isEmpty")

    override val lineId: IntVar
    override val lineIdDomainMap: Map<Int, Literal>
    private val beltOptions = config.getOptions()
    override fun getOptions(): Map<CardinalDirection, MultiMap<BeltType, BeltLineId>> = beltOptions

    override val canBeEmpty: Boolean = config.canBeEmpty
    override val propagatesForward: Boolean = config.propagatesForward
    override val propagatesBackward: Boolean = config.propagatesBackward


    init {
        val possibleBeltIds = mutableSetOf<Long>()
        for (a in beltOptions.values) for (b in a.values) for (c in b) {
            possibleBeltIds.add(c.toLong())
        }
        if (possibleBeltIds.isEmpty()) {
            this.lineId = cp.falseLiteral() as IntVar
            this.lineIdDomainMap = emptyMap()
        } else {
            this.lineId = cp.newIntVarFromDomain(Domain.fromValues(possibleBeltIds.toLongArray()), "beltLineId")

            val beltIdDomainMap = mutableMapOf<Int, Literal>()
            for (beltId in possibleBeltIds) {
                val lit = cp.newBoolVar("lineId_${beltId}")
                cp.addEquality(lineId, beltId.toLong()).onlyEnforceIf(lit)
                cp.addDifferent(lineId, beltId.toLong()).onlyEnforceIf(!lit)
                beltIdDomainMap[beltId.toInt()] = lit
            }
            this.lineIdDomainMap = beltIdDomainMap
        }
    }

    override val selectedBelt: BeltSelectVars = beltOptions.mapValues { (direction, beltTypes) ->
        beltTypes.mapValues { (beltType, ids) ->
            val selectedVar = cp.newBoolVar("selected_${beltType}_${direction}")
            cp.addBoolOr(ids.map { lineIdDomainMap[it]!! })
                .onlyEnforceIf(selectedVar)
            selectedVar
        }
    }

    init {
        cp.addAtLeastOne(selectedBelt.values.flatMap { it.values } + isEmpty)
        if (!canBeEmpty) cp.addEquality(isEmpty, 0)
    }

    override val hasOutputIn: Map<CardinalDirection, Literal> =
        this.selectedBelt.mapValuesNotNull { (direction, beltTypeSelected) ->
            val withOutput = beltTypeSelected.keys.filter { it.hasOutput }
            if (withOutput.isEmpty()) return@mapValuesNotNull null

            val hasOutVar = cp.newBoolVar("hasOutputIn_${direction}")
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

            val hasInVar = cp.newBoolVar("hasInputIn_${direction}")
            cp.addBoolOr(withInput.mapToArray { beltTypeSelected[it]!! })
                .onlyEnforceIf(hasInVar)
            hasInVar
        }

    init {
        // this is redundant, but it might help cp
        cp.addAtMostOne(hasOutputIn.values)
    }
}
