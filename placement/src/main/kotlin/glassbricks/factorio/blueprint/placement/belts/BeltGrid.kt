package glassbricks.factorio.blueprint.placement.belts

import com.google.ortools.sat.CpModel
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.Disjunction
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.WithCp
import glassbricks.factorio.blueprint.placement.asLit


class BeltGrid(
    override val cp: CpModel,
    tiles: Iterable<TilePosition>,
    val undergroundMaxLength: Int = 5,
) : WithCp {
    val tiles: Map<TilePosition, BeltTile> =
        tiles.associateWith { BeltTile(this, it) }

    val emptyTile: BeltTileVars = object : BeltTileVars {
        override val cp: CpModel get() = this@BeltGrid.cp
        private val falseLiteral = cp.falseLiteral().asLit()
        private val trueLiteral = cp.trueLiteral().asLit()
        private val falseDirections = PerDirection { falseLiteral }
        override val isEmpty: Literal get() = trueLiteral
        override val isBelt: Literal get() = falseLiteral
        override val isInputUndergroundIn: PerDirectionVars get() = falseDirections
        override val isOutputUndergroundIn: PerDirectionVars get() = falseDirections
        override val isInputUnderground: Disjunction get() = falseLiteral
        override val isOutputUnderground: Disjunction get() = falseLiteral
        override val mainInputDirection: PerDirectionVars get() = falseDirections
        override val outputDirection: PerDirectionVars get() = falseDirections
        override val isUndergroundConnection: PerDirectionVars get() = falseDirections
    }

    fun getTile(position: TilePosition): BeltTile? = tiles[position]
    fun getTile(x: Int, y: Int): BeltTile? = getTile(TilePosition(x, y))
    fun getTileShifted(position: TilePosition, direction: CardinalDirection, amt: Int = 1): BeltTile? =
        getTile(position.shifted(direction, amt))

    internal var mutable = true
        private set


    fun finalizeConstraints() {
        if (!mutable) return
        mutable = false
        for (tile in tiles.values) {
            addBasicTileConstraints(tile, tile.beltType)
            this.enforceInOut()
            this.enforceUndergroundConnections()
            this.enforceUndergroundMaxLength()
        }
    }
}


enum class BeltTileType {
    ConnectingBelt,
    FixedInputBelt,
    FixedOutputSpot
    // todo: handle "hanging" belts/undergrounds, sideloaded belts.
}

class BeltTile(
    val grid: BeltGrid,
    val position: TilePosition,
) : BeltTileVars {
    override val cp: CpModel get() = grid.cp
    private fun newLit(name: String) = cp.newBoolVar(name).asLit()
    private fun litPerDirection(name: String): PerDirectionVars = PerDirection { newLit("$name-$it") }

    override val isBelt = newLit("isBelt")
    override val isEmpty = newLit("isEmpty")

    override val isInputUndergroundIn = litPerDirection("isInputUndergroundIn")
    override val isOutputUndergroundIn = litPerDirection("isOutputUndergroundIn")

    override val isInputUnderground: Disjunction = Disjunction(isInputUndergroundIn)
    override val isOutputUnderground: Disjunction = Disjunction(isOutputUndergroundIn)
    override val mainInputDirection: PerDirectionVars = litPerDirection("inputDirection")
    override val outputDirection: PerDirectionVars = litPerDirection("outputDirection")
    override val isUndergroundConnection: PerDirectionVars = litPerDirection("hasUndergroundConnection")

    var beltType: BeltTileType = BeltTileType.ConnectingBelt
        set(value) {
            check(grid.mutable) { "Cannot change belt type after finalizing constraints" }
            field = value
        }
}
