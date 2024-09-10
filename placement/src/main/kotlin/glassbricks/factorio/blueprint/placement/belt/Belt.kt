package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.IntVar
import com.google.ortools.sat.Literal
import com.google.ortools.util.Domain
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.MultiTable
import glassbricks.factorio.blueprint.placement.OptionalEntityPlacement
import glassbricks.factorio.blueprint.placement.Table
import glassbricks.factorio.blueprint.placement.toFactorioDirection

class BeltPlacement(
    val beltType: BeltType,
    val placement: OptionalEntityPlacement<*>,
    val lineIds: Collection<BeltLineId>,
) {
    val selected get() = placement.selected
}

interface Belt : BeltCommon {
    val lineId: IntVar
    val lineIdDomainMap: Map<Int, Literal>
    val selectedBelt: Table<CardinalDirection, BeltType, BeltPlacement>
    val hasOutputIn: Map<CardinalDirection, Literal>
    val hasInputIn: Map<CardinalDirection, Literal>
}


internal class BeltImpl(
    model: EntityPlacementModel,
    config: BeltCommon,
) : Belt {
    override val pos = config.pos
    override val lineId: IntVar
    override val lineIdDomainMap: Map<Int, Literal>
    private val beltOptions = config.getOptions()
    override fun getOptions(): MultiTable<CardinalDirection, BeltType, BeltLineId> = beltOptions

    override val canBeEmpty: Boolean = config.canBeEmpty
    override val propagatesForward: Boolean = config.propagatesForward
    override val propagatesBackward: Boolean = config.propagatesBackward

    init {
        val possibleBeltIds = mutableSetOf<Long>()
        for (values in beltOptions.values) for (c in values) {
            possibleBeltIds.add(c.toLong())
        }
        val cp = model.cp
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

    override val selectedBelt: Table<CardinalDirection, BeltType, BeltPlacement> =
        beltOptions.mapValues { (entry, lineIds) ->
            val (direction, beltType) = entry
            val placement = model.createPlacement(
                pos,
                direction,
                beltType,
            )

            model.cp.addBoolOr(lineIds.map { lineIdDomainMap[it]!! })
                .onlyEnforceIf(placement.selected)

            BeltPlacement(beltType, placement, lineIds)
        }

    init {
        if (canBeEmpty) {
            model.cp.addAtMostOne(selectedBelt.values.map { it.selected })
        } else {
            model.cp.addExactlyOne(selectedBelt.values.map { it.selected })
        }
    }

    override val hasOutputIn: Map<CardinalDirection, Literal> = buildMap {
        for (direction in CardinalDirection.entries) {
            val selected = selectedBelt
                .entries
                .filter { it.key.first == direction && it.key.second.hasOutput }
                .map { it.value.selected }
            if (selected.isEmpty()) continue
            val hasOutVar = model.cp.newBoolVar("hasOutputIn_${direction}")
            // has out <=> any selected
            model.cp.addBoolOr(selected).onlyEnforceIf(hasOutVar)
            for (literal in selected) model.cp.addImplication(literal, hasOutVar)
            put(direction, hasOutVar)
        }
    }

    // todo: handle sideloading, which makes hasInputIn more complicated
    override val hasInputIn: Map<CardinalDirection, Literal> = buildMap {
        for (direction in CardinalDirection.entries) {
            val selected = selectedBelt
                .entries
                .filter { it.key.first == direction && it.key.second.hasInput }
                .map { it.value.selected }
            if (selected.isEmpty()) continue
            val hasInVar = model.cp.newBoolVar("hasInputIn_${direction}")
            // has in <=> any selected
            model.cp.addBoolOr(selected).onlyEnforceIf(hasInVar)
            for (literal in selected) model.cp.addImplication(literal, hasInVar)
            put(direction, hasInVar)
        }
    }
}

internal fun EntityPlacementModel.createPlacement(
    tile: TilePosition,
    direction: CardinalDirection,
    type: BeltType,
): OptionalEntityPlacement<*> = when (type) {
    is BeltType.Belt -> addPlacement(
        type.prototype,
        tile.center(),
        direction.toFactorioDirection(),
    )

    is BeltType.Underground ->
        createBpEntity(type.prototype, tile.center(), direction.toFactorioDirection())
            .apply {
                this as UndergroundBelt
                ioType = when (type) {
                    is BeltType.InputUnderground -> IOType.Input
                    is BeltType.OutputUnderground -> IOType.Output
                }
            }
            .let { addPlacement(it) }
}
