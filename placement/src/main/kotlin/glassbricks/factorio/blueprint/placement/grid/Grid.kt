package glassbricks.factorio.blueprint.placement.grid

import com.google.ortools.sat.CpModel
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.WithCp


class GridConfig {
    private val cells: MutableMap<TilePosition, CellConfig> = mutableMapOf()
    fun cellConfig(position: TilePosition): CellConfig = cells.getOrPut(position) { CellConfig() }
    fun cellConfig(x: Int, y: Int): CellConfig = cellConfig(TilePosition(x, y))

    fun toVars(cp: CpModel): GridVars {
        val grid = cells.mapValuesTo(hashMapOf()) { (_, config) -> CellVarsImpl(cp, config) }
        return GridVars(cp, grid)
    }
}

class GridVars internal constructor(
    override val cp: CpModel,
    grid: MutableMap<TilePosition, CellVarsImpl>,
) : WithCp {
    init {
        ensureUndergroundConnectors(grid)
        addBeltConstraints()
    }

    val grid: Map<TilePosition, CellVars> = grid
}

private fun WithCp.ensureUndergroundConnectors(grid: MutableMap<TilePosition, CellVarsImpl>) {
    for ((tile, cell) in grid.entries.toList()) for ((direction, beltOptions) in cell.belt.getOptions()) for (type in beltOptions.keys) {
        val mul = when (type) {
            is BeltType.InputUnderground -> 1
            is BeltType.OutputUnderground -> -1
            else -> continue
        }
        val prototype = type.prototype
        for (dist in 1..prototype.max_distance.toInt()) {
            val nextTile = tile.shifted(direction, dist * mul)
            grid.getOrPut(nextTile) { CellVarsImpl(cp, CellConfig()) }.ensureUgConnector(cp, direction, prototype)
        }
    }
    for ((_, cell) in grid) cell.constrainUgId(cp)
}

private fun GridVars.addBeltConstraints() {
    for ((tile, cell) in grid) {
        val belt = cell.belt
        for ((direction, beltTypes) in belt.selectedBelt) {
            for ((type, thisSelected) in beltTypes) {
                if (type is BeltType.Underground) constrainUnderground(tile, type, direction, thisSelected)
            }
            constrainBeltPropagation(tile, direction)
        }
    }
}

private fun GridVars.constrainBeltPropagation(tile: TilePosition, direction: CardinalDirection) {
    val belt = grid[tile]!!.belt
    val thisId = belt.lineId
    if (belt.propagatesForward) {
        val thisOutput = belt.hasOutputIn[direction]
        if (thisOutput != null) {
            val nextCell = grid[tile.shifted(direction)]?.belt
            val nextCellInput = nextCell?.hasInputIn[direction] ?: cp.falseLiteral()
            cp.addImplication(thisOutput, nextCellInput)
            val nextId = nextCell?.lineId ?: cp.newConstant(0)
            cp.addEquality(thisId, nextId).onlyEnforceIf(thisOutput)
        }
    }
    if (belt.propagatesBackward) {
        val thisInput = belt.hasInputIn[direction]
        if (thisInput != null) {
            val prevCell = grid[tile.shifted(direction.oppositeDir())]?.belt
            val prevCellOutput = prevCell?.hasOutputIn[direction] ?: cp.falseLiteral()
            cp.addImplication(thisInput, prevCellOutput)
            val prevId = prevCell?.lineId ?: cp.newConstant(0)
            cp.addEquality(thisId, prevId).onlyEnforceIf(thisInput)
        }
    }
}

private fun GridVars.constrainUnderground(
    tile: TilePosition,
    type: BeltType.Underground,
    direction: CardinalDirection,
    thisSelected: Literal,
) {
    if (type.isIsolated) TODO()
    val belt = grid[tile]!!.belt

    val ugDir = when (type) {
        is BeltType.InputUnderground -> direction
        is BeltType.OutputUnderground -> direction.oppositeDir()
    }

    val nextTile = tile.shifted(ugDir)
    val oppositeType = type.opposite()!!

    val nextCell = grid[nextTile]?.belt
        ?: throw AssertionError("Should have been created by ensureUndergroundConnectors")

    // ug cannot exist at same time as connector on same axis
    belt.ugConnectorSelected[direction]?.get(type.prototype)?.let {
        cp.addImplication(thisSelected, !it)
    }
    belt.ugConnectorSelected[direction.oppositeDir()]?.get(type.prototype)?.let {
        cp.addImplication(it, !thisSelected)
    }

    // if selected, then either:
    // - forward tile underground connector is selected, and has same id
    // - forward tile is output underground, and has same id
    // sel => ugSelected || tileNext is output
    // sel && !(tileNext is output) => connId == thisId
    // sel && tileNext is output => tileNext.id == thisId

    val ugConnectorSelected = nextCell.ugConnectorSelected[direction]!![type.prototype]!!
    val ugConnectorId = nextCell.ugConnectorId[direction.axis]!![type.prototype]!!
    val nextUgSelected = nextCell.selectedBelt[direction]?.get(oppositeType) ?: cp.falseLiteral()
    val nextId = nextCell.lineId

    cp.addBoolOr(
        listOf(
            !thisSelected,
            ugConnectorSelected,
            nextUgSelected,
        )
    )
    cp.addEquality(belt.lineId, ugConnectorId).apply {
        onlyEnforceIf(thisSelected)
        onlyEnforceIf(!nextUgSelected)
    }
    cp.addEquality(belt.lineId, nextId).apply {
        onlyEnforceIf(thisSelected)
        onlyEnforceIf(nextUgSelected)
    }

    // ug <= max underground distance: one underground in range selected
    val withinDistance = (1..type.prototype.max_distance.toInt()).mapNotNull { dist ->
        val nextTile = tile.shifted(ugDir, dist)
        grid[nextTile]?.belt?.selectedBelt?.get(direction)?.get(oppositeType)
    } + !thisSelected
    cp.addBoolOr(withinDistance)
}
