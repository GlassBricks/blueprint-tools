package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpModel
import com.google.ortools.sat.IntVar
import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.json.IOType
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.WithCp
import glassbricks.factorio.blueprint.placement.addEquality
import glassbricks.factorio.blueprint.placement.shifted
import glassbricks.factorio.blueprint.placement.toFactorioDirection
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype

class BeltGrid internal constructor(
    override val cp: CpModel,
    grid: MutableMap<TilePosition, BeltVarsImpl>,
) : WithCp {
    val map: Map<TilePosition, BeltVars> = grid

    init {
        ensureUndergroundConnectors(grid)
        addBeltConstraints()
    }

    operator fun get(tile: TilePosition): BeltVars? = map[tile]
}

private fun WithCp.ensureUndergroundConnectors(grid: MutableMap<TilePosition, BeltVarsImpl>) {
    for ((tile, cell) in grid.entries.toList()) for ((direction, beltOptions) in cell.getOptions()) for (type in beltOptions.keys) {
        val mul = when (type) {
            is BeltType.InputUnderground -> 1
            is BeltType.OutputUnderground -> -1
            else -> continue
        }
        val prototype = type.prototype
        for (dist in 1..prototype.max_distance.toInt()) {
            val nextTile = tile.shifted(direction, dist * mul)
            grid.getOrPut(nextTile) { BeltVarsImpl(cp, BeltConfigImpl()) }.ensureUgConnector(cp, direction, prototype)
        }
    }
    for ((_, cell) in grid) cell.constrainUgId(cp)
}

private fun BeltGrid.addBeltConstraints() {
    for ((tile, belt) in map) {
        for (direction in CardinalDirection.entries) {
            constrainUndergroundLink(tile, direction)
        }
        for ((direction, beltTypes) in belt.selectedBelt) {
            constrainBeltPropagation(tile, direction)
            for ((type, thisSelected) in beltTypes) {
                if (type is BeltType.Underground) constrainUnderground(tile, type, direction, thisSelected)
            }
        }
    }
}


private fun BeltGrid.constrainBeltPropagation(tile: TilePosition, direction: CardinalDirection) {
    val belt = map[tile]!!
    val thisId = belt.lineId
    val nextCell = map[tile.shifted(direction)]
    val prevCell = map[tile.shifted(direction.oppositeDir())]

    fun constrainEqual(
        nextCell: BeltVars?,
        thisIsOutput: Boolean,
    ) {
        val thisVar = if (thisIsOutput) belt.hasOutputIn[direction] else belt.hasInputIn[direction]
        if (thisVar != null) {
            val nextVar = (if (thisIsOutput) nextCell?.hasInputIn[direction] else nextCell?.hasOutputIn[direction])
                ?: cp.falseLiteral()
            cp.addImplication(thisVar, nextVar)
            val nextId = nextCell?.lineId ?: cp.newConstant(0)
            cp.addEquality(thisId, nextId).onlyEnforceIf(thisVar)
        }
    }
    if (belt.propagatesForward) constrainEqual(nextCell, true)
    if (belt.propagatesBackward) constrainEqual(prevCell, false)
    // ug connectors always propagate
}

private fun BeltGrid.constrainUndergroundLink(
    tile: TilePosition,
    direction: CardinalDirection,
) {
    val belt = map[tile]!!
    val nextCell = map[tile.shifted(direction)]
    val prevCell = map[tile.shifted(direction.oppositeDir())]
    for ((prototype, selected) in belt.ugConnectorSelected[direction].orEmpty()) {
        addUndergroundLink(
            prototype = prototype,
            direction = direction,
            thisSelected = selected,
            thisId = belt.ugConnectorId[direction.axis]!![prototype]!!,
            nextCell = nextCell,
            nextUgType = BeltType.OutputUnderground(prototype)
        )
        addUndergroundLink(
            prototype = prototype,
            direction = direction,
            thisSelected = selected,
            thisId = belt.ugConnectorId[direction.axis]!![prototype]!!,
            nextCell = prevCell,
            nextUgType = BeltType.InputUnderground(prototype)
        )
    }
}

private fun BeltGrid.constrainUnderground(
    tile: TilePosition,
    type: BeltType.Underground,
    direction: CardinalDirection,
    thisSelected: Literal,
) {
    val isIsolated = type.isIsolated

    val belt = map[tile]!!
    val ugDir = when (type) {
        is BeltType.InputUnderground -> direction
        is BeltType.OutputUnderground -> direction.oppositeDir()
    }

    // ug cannot exist at same time as connector in same axis
    belt.ugConnectorSelected[direction]?.get(type.prototype)?.let {
        cp.addImplication(thisSelected, !it)
    }
    belt.ugConnectorSelected[direction.oppositeDir()]?.get(type.prototype)?.let {
        cp.addImplication(it, !thisSelected)
    }

    val nextTile = tile.shifted(ugDir)
    val nextCell = map[nextTile]

    addUndergroundLink(
        prototype = type.prototype,
        direction = direction,
        thisSelected = thisSelected,
        thisId = belt.lineId,
        nextCell = nextCell,
        nextUgType = type.opposite(isolated = false),
        invert = isIsolated
    )

    // ug <= max underground distance: one underground in range selected
    // if inverted: enforce _not_ selected
    val oppositeSameIsolated = type.opposite(isolated = isIsolated)
    val oppositeWithinDist = (1..type.prototype.max_distance.toInt()).mapNotNull { dist ->
        val nextTile = tile.shifted(ugDir, dist)
        map[nextTile]?.selectedBelt?.get(direction)?.get(oppositeSameIsolated)
    }
    if (!isIsolated) {
        cp.addBoolOr(oppositeWithinDist).onlyEnforceIf(thisSelected)
    } else {
        cp.addBoolAnd(oppositeWithinDist.map { !it }).onlyEnforceIf(thisSelected)
    }
}

private fun BeltGrid.addUndergroundLink(
    prototype: UndergroundBeltPrototype,
    direction: CardinalDirection,
    thisSelected: Literal,
    thisId: IntVar,
    nextCell: BeltVars?,
    nextUgType: BeltType.Underground,
    invert: Boolean = false,
) {
    if (nextCell == null) {
        if (!invert) cp.addEquality(thisSelected, false)
        return
    }
    val ugConnectorSelected = nextCell.ugConnectorSelected[direction]?.get(prototype) ?: cp.falseLiteral()
    // if selected, then one of the following
    // - next tile is ug connector and has same id
    // - next tile is underground in/out, and has same id
    // sel => ugSelected || tileNext is output
    // sel && !(tileNext is output) => connId == thisId
    // sel && tileNext is output => tileNext.id == thisId

    val ugConnectorId = nextCell.ugConnectorId[direction.axis]?.get(prototype) ?: cp.newConstant(0)
    val nextUgSelected = nextCell.selectedBelt[direction]?.get(nextUgType) ?: cp.falseLiteral()
    val nextId = nextCell.lineId

    if (!invert) {
        cp.addBoolOr(
            listOf(
                !thisSelected,
                ugConnectorSelected,
                nextUgSelected,
            )
        )
        cp.addEquality(thisId, ugConnectorId).apply {
            onlyEnforceIf(thisSelected)
            onlyEnforceIf(!nextUgSelected)
        }
        cp.addEquality(thisId, nextId).apply {
            onlyEnforceIf(thisSelected)
            onlyEnforceIf(nextUgSelected)
        }
    } else {
        // otherwise, there must _not_ be a connection
        cp.addBoolAnd(listOf(!ugConnectorSelected, !nextUgSelected)).onlyEnforceIf(thisSelected)
    }
}

internal fun EntityPlacementModel.addBeltPlacements(grid: BeltGrid) {
    for ((tile, belt) in grid.map) {
        for ((direction, beltTypes) in belt.selectedBelt) {
            for ((type, selected) in beltTypes) when (type) {
                is BeltType.Belt -> addPlacement(
                    type.prototype,
                    tile.center(),
                    direction.toFactorioDirection(),
                    selected = selected
                )

                is BeltType.Underground -> addPlacement(
                    createBpEntity(type.prototype, tile.center(), direction.toFactorioDirection())
                        .apply {
                            this as UndergroundBelt
                            ioType = when (type) {
                                is BeltType.InputUnderground -> IOType.Input
                                is BeltType.OutputUnderground -> IOType.Output
                            }
                            addPlacement(this, selected = selected)
                        }
                )
            }
        }
    }
}

fun EntityPlacementModel.addBeltGrid(grid: BeltGridConfig): BeltGrid {
    val vars = grid.applyTo(cp)
    addBeltPlacements(vars)
    return vars
}
