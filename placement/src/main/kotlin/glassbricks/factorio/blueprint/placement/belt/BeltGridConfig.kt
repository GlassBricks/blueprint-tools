package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpModel
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.shifted


open class BeltGridConfig {
    private val cells: MutableMap<TilePosition, MutableBeltConfig> = mutableMapOf()
    operator fun get(position: TilePosition): MutableBeltConfig = cells.getOrPut(position) { BeltConfigImpl() }
    fun get(x: Int, y: Int): BeltConfig = get(TilePosition(x, y))

    fun compile(cp: CpModel): GridVars {
        val grid = cells.mapValuesTo(hashMapOf()) { (_, config) -> BeltVarsImpl(cp, config) }
        return GridVars(cp, grid)
    }

    private var nextBeltId: BeltLineId = 1
    fun newBeltId(): BeltLineId = nextBeltId++
}

fun BeltGridConfig.addBeltLine(
    start: TilePosition,
    direction: CardinalDirection,
    length: Int,
    beltTiers: List<BeltTier>,
    id: BeltLineId = newBeltId(),
) {
    val startCell = get(start)
    startCell.makeLineStart(direction, id)

    val endTile = start.shifted(direction, length - 1)
    val endCell = get(endTile)
    endCell.makeLineEnd(direction, id)

    for (tier in beltTiers) {
        val belt = tier.belt
        val inputUG = tier.inputUg
        val outputUg = tier.outputUg

        startCell.addOption(direction, belt, id)
        startCell.addOption(direction, inputUG, id)
        endCell.addOption(direction, belt, id)
        endCell.addOption(direction, outputUg, id)

        for (dist in 1..<length - 1) {
            val tile = start.shifted(direction, dist)
            val cell = get(tile)
            cell.addOption(direction, belt, id)
            cell.addOption(direction, inputUG, id)
            cell.addOption(direction, outputUg, id)
        }
    }
}
