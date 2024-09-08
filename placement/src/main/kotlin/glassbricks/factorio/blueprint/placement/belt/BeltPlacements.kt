package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.Literal
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.addEquality
import glassbricks.factorio.blueprint.placement.get
import glassbricks.factorio.blueprint.placement.shifted

class BeltPlacements internal constructor(
    val model: EntityPlacementModel,
    val beltLines: Map<BeltLineId, BeltLine>,
    tiles: MutableMap<TilePosition, BeltImpl>,
) {
    val tiles: Map<TilePosition, Belt> = tiles
    val cp get() = model.cp

    init {
        addGridConstraints()
    }

    operator fun get(tile: TilePosition): Belt? = tiles[tile]
}

private fun BeltPlacements.addGridConstraints() {
    for ((tile, belt) in tiles) {
        for (direction in CardinalDirection.entries) constrainBeltPropagation(tile, direction)
        for ((entry, thisPlacement) in belt.selectedBelt) {
            val (direction, type) = entry
            if (type is BeltType.Underground)
                constrainUnderground(tile, direction, type, thisPlacement.selected)
        }
    }
}


private fun BeltPlacements.constrainBeltPropagation(tile: TilePosition, direction: CardinalDirection) {
    val belt = tiles[tile]!!
    val thisId = belt.lineId
    val nextCell = tiles[tile.shifted(direction)]
    val prevCell = tiles[tile.shifted(direction.oppositeDir())]

    fun constrainEqual(
        nextCell: Belt?,
        thisIsOutput: Boolean,
    ) {
        val thisVar = if (thisIsOutput) belt.hasOutputIn[direction] else belt.hasInputIn[direction]
        if (thisVar == null) return
        val nextVar = if (thisIsOutput) nextCell?.hasInputIn[direction] else nextCell?.hasOutputIn[direction]
        if (nextVar == null) {
            cp.addEquality(thisVar, false)
            return
        }
        cp.addImplication(thisVar, nextVar)
        cp.addEquality(thisId, nextCell!!.lineId).onlyEnforceIf(thisVar)
    }
    if (belt.propagatesForward) constrainEqual(nextCell, true)
    if (belt.propagatesBackward) constrainEqual(prevCell, false)
}

private fun BeltPlacements.constrainMustNotOutputIn(
    tile: TilePosition,
    direction: CardinalDirection,
) {
    val belt = tiles[tile]!!
    val hasOutput = belt.hasOutputIn[direction] ?: return

    val nextTile = tile.shifted(direction)
    val nextBelt = tiles[nextTile] ?: return
    // allowed if:
    // - opposite direction
    // - is output underground in same direction
    // - is input underground in opposite direction (covered by 1st item)
    // all other not allowed
    for ((entry, placement) in nextBelt.selectedBelt) {
        val (nextDirection, nextType) = entry
        val allowed = direction == nextDirection.oppositeDir()
                || (direction == nextDirection && nextType is BeltType.OutputUnderground)
        if (!allowed) {
            cp.addImplication(hasOutput, !placement.selected)
        }
    }
}

private fun BeltPlacements.constrainUnderground(
    tile: TilePosition,
    direction: CardinalDirection,
    ugType: BeltType.Underground,
    thisSelected: Literal,
) {
    if (ugType.isIsolated) constrainIsolatedUnderground(tile, ugType, direction, thisSelected)
    else constrainNormalUnderground(tile, ugType, direction, thisSelected)
}

private fun BeltPlacements.constrainNormalUnderground(
    tile: TilePosition,
    thisType: BeltType.Underground,
    thisDirection: CardinalDirection,
    thisSelected: Literal,
) {
    val belt = tiles[tile]!!
    val ugDir = when (thisType) {
        is BeltType.InputUnderground -> thisDirection
        is BeltType.OutputUnderground -> thisDirection.oppositeDir()
    }
    val oppositeType = thisType.oppositeNotIsolated()!!

    // one output underground within range be selected
    // if it matches, then the line id must be equal
    // there cannot be anything in between that cuts off the connection
    val previousPairs = mutableListOf<Literal>()
    for (dist in 1..thisType.prototype.max_distance.toInt()) {
        val otherBelt = tiles[tile.shifted(ugDir, dist)] ?: continue

//        val pairSelected = otherBelt.selectedBelt[thisDirection]?.get(oppositeType)
        val pairSelected = otherBelt.selectedBelt[thisDirection, oppositeType]?.selected
        if (pairSelected != null) {
            cp.addEquality(belt.lineId, otherBelt.lineId).apply {
                onlyEnforceIf(thisSelected)
                onlyEnforceIf(pairSelected)
                for (previousPair in previousPairs) onlyEnforceIf(!previousPair)
            }
        }

        fun disallow(type: BeltType.Underground, direction: CardinalDirection) {
            val lit = otherBelt.selectedBelt[direction, type]?.selected ?: return
            cp.addEquality(lit, false).apply {
                onlyEnforceIf(thisSelected)
                for (previous in previousPairs) onlyEnforceIf(!previous)
            }
        }
        disallow(thisType, thisDirection) // another input underground, would pair with the next instead
        disallow(oppositeType, thisDirection.oppositeDir()) // same as above, flipped
        disallow(thisType, thisDirection.oppositeDir()) // paired underground in the wrong direction

        if (pairSelected != null) previousPairs.add(pairSelected)
    }
    cp.addAtLeastOne(previousPairs).onlyEnforceIf(thisSelected)
}

private fun BeltPlacements.constrainIsolatedUnderground(
    tile: TilePosition,
    thisType: BeltType.Underground,
    direction: CardinalDirection,
    thisSelected: Literal,
) {
    // constrain such that the underground does _not_ have a pair
    val ugDir = when (thisType) {
        is BeltType.InputUnderground -> direction
        is BeltType.OutputUnderground -> direction.oppositeDir()
    }
    val breaksPair = mutableListOf<Literal>()
    for (dist in 1..thisType.prototype.max_distance.toInt()) {
        val otherBelt = tiles[tile.shifted(ugDir, dist)] ?: continue
        val breaksPairThisTile = mutableListOf<Literal>()
        fun disallow(lit: Literal) {
            cp.addEquality(lit, false).apply {
                onlyEnforceIf(thisSelected)
                for (previous in breaksPair) onlyEnforceIf(!previous)
            }
        }
        for ((entry, placement) in otherBelt.selectedBelt) {
            val (otherDirection, otherType) = entry
            if (otherDirection != direction) continue
            if (thisType.prototype != otherType.prototype) continue
            if (thisType.hasInput == otherType.hasInput)
                breaksPairThisTile.add(placement.selected) // same direction, same type -- breaks pair
            else
                disallow(placement.selected) // would pair with this one
        }
        for ((entry, placement) in otherBelt.selectedBelt) {
            val (otherDirection, otherType) = entry
            if (otherDirection != direction.oppositeDir()) continue
            val otherSelected = placement.selected
            if (thisType.prototype != otherType.prototype) continue
            if (thisType.hasInput == otherType.hasInput) // would pair with this one
                disallow(otherSelected)
            else
                breaksPairThisTile.add(otherSelected) // same direction, same type -- breaks pair
        }
        breaksPair += breaksPairThisTile
    }
}
