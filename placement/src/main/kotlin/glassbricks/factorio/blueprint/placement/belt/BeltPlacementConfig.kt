package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.shifted
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class BeltPlacementConfig {
    private val cells: MutableMap<TilePosition, BeltConfig> = mutableMapOf()
    operator fun get(position: TilePosition): BeltConfig = cells.getOrPut(position) { BeltConfigImpl(position) }
    fun get(x: Int, y: Int): BeltCommon = get(TilePosition(x, y))

    private var nextBeltId: BeltLineId = 1
    fun newLineId(): BeltLineId = nextBeltId++

    private val beltLines: MutableMap<BeltLineId, BeltLine> = mutableMapOf()
    internal fun recordLine(id: BeltLineId, line: BeltLine) {
        beltLines[id] = line
    }

    /**
     * Doesn't actually create any entity placements
     */
    internal fun addTo(model: EntityPlacementModel): BeltPlacements {
        logger.info { "Applying belt grid config to cp" }
        val grid = cells
            .mapValuesTo(HashMap()) { (_, config) -> BeltImpl(model, config) }
        return BeltPlacements(model, beltLines, grid)
    }

}

fun EntityPlacementModel.addBeltPlacements(grid: BeltPlacementConfig): BeltPlacements = grid.addTo(this)

// only kept around for testing reasons
internal fun BeltPlacementConfig.addBeltLine(
    start: TilePosition,
    direction: CardinalDirection,
    length: Int,
    beltTiers: Set<BeltTier>,
    id: BeltLineId = newLineId(),
): BeltLineId {
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
    return id
}
