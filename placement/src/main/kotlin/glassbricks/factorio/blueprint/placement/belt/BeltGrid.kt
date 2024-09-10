package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.shifted

interface BeltGridCommon {
    val tiles: Map<TilePosition, BeltTile>
    val beltLines: Map<BeltLineId, BeltLine>
}

class BeltGrid : BeltGridCommon {
    private val _tiles: MutableMap<TilePosition, BeltConfig> = mutableMapOf()
    override val tiles: Map<TilePosition, BeltConfig> get() = _tiles
    operator fun get(position: TilePosition): BeltConfig = _tiles.getOrPut(position) { BeltConfig(position) }
    fun get(x: Int, y: Int): BeltTile = get(TilePosition(x, y))

    private var nextBeltId: BeltLineId = 1
    fun newLineId(): BeltLineId = nextBeltId++

    private val _beltLines: MutableMap<BeltLineId, BeltLine> = mutableMapOf()
    override val beltLines: Map<BeltLineId, BeltLine> get() = _beltLines

    internal fun addLine(id: BeltLineId, line: BeltLine) {
        _beltLines[id] = line
    }
}

// only kept around for testing reasons
internal fun BeltGrid.addBeltLine(
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
