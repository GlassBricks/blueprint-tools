package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpModel
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.shifted
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

open class GridConfig {
    private val cells: MutableMap<TilePosition, MutableBeltConfig> = mutableMapOf()
    operator fun get(position: TilePosition): MutableBeltConfig = cells.getOrPut(position) { BeltConfigImpl() }
    fun get(x: Int, y: Int): BeltConfig = get(TilePosition(x, y))

    /**
     * Doesn't actually create any entity placements
     */
    internal fun applyTo(cp: CpModel): Grid {
        logger.info { "Applying belt grid config to cp" }
        val grid = cells.mapValuesTo(HashMap()) { (_, config) -> BeltImpl(cp, config) }
        return Grid(cp, grid)
    }

    private var nextBeltId: BeltLineId = 1
    fun newLineId(): BeltLineId = nextBeltId++
}

fun GridConfig.addBeltLine(
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
