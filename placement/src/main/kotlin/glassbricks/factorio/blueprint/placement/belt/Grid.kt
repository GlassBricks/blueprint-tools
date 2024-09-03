package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.sat.CpModel
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

class Grid internal constructor(
    override val cp: CpModel,
    grid: MutableMap<TilePosition, BeltImpl>,
) : WithCp {
    val belts: Map<TilePosition, Belt> = grid

    init {
        addGridConstraints()
    }

    operator fun get(tile: TilePosition): Belt? = belts[tile]
}

private fun Grid.addGridConstraints() {
    for ((tile, belt) in belts) {
        for (direction in CardinalDirection.entries) constrainBeltPropagation(tile, direction)
        belt.mustNotOutputIn?.let { constrainMustNotOutputIn(tile, it) }
        for ((direction, beltTypes) in belt.selectedBelt) {
            for ((type, thisSelected) in beltTypes) {
                if (type is BeltType.Underground)
                    constrainUnderground(tile, direction, type, thisSelected)
            }
        }
    }
}


private fun Grid.constrainBeltPropagation(tile: TilePosition, direction: CardinalDirection) {
    val belt = belts[tile]!!
    val thisId = belt.lineId
    val nextCell = belts[tile.shifted(direction)]
    val prevCell = belts[tile.shifted(direction.oppositeDir())]

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

private fun Grid.constrainMustNotOutputIn(
    tile: TilePosition,
    direction: CardinalDirection,
) {
    val belt = belts[tile]!!
    val nextTile = tile.shifted(direction)
    val nextBelt = belts[nextTile] ?: return
    // allowed if:
    // - opposite direction
    // - is output underground in same direction
    // - is input underground in opposite direction (covered by 1st item)
    // all other not allowed
    for ((nextDirection, beltTypes) in nextBelt.selectedBelt) {
        for ((type, selected) in beltTypes) {
            val allowed = direction == nextDirection.oppositeDir()
                    || (type is BeltType.OutputUnderground && direction == nextDirection)
            if (!allowed) {
                cp.addEquality(selected, false).onlyEnforceIf(belt.hasOutputIn[direction] ?: cp.falseLiteral())
            }
        }
    }
}

private fun Grid.constrainUnderground(
    tile: TilePosition,
    direction: CardinalDirection,
    ugType: BeltType.Underground,
    thisSelected: Literal,
) {
    if (ugType.isIsolated) constrainIsolatedUnderground(tile, ugType, direction, thisSelected)
    else constrainNormalUnderground(tile, ugType, direction, thisSelected)
}

private fun Grid.constrainNormalUnderground(
    tile: TilePosition,
    thisType: BeltType.Underground,
    thisDirection: CardinalDirection,
    thisSelected: Literal,
) {
    val belt = belts[tile]!!
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
        val otherBelt = belts[tile.shifted(ugDir, dist)] ?: continue

        val pairSelected = otherBelt.selectedBelt[thisDirection]?.get(oppositeType)
        if (pairSelected != null) {
            cp.addEquality(belt.lineId, otherBelt.lineId).apply {
                onlyEnforceIf(thisSelected)
                onlyEnforceIf(pairSelected)
                for (previousPair in previousPairs) onlyEnforceIf(!previousPair)
            }
        }

        fun disallow(type: BeltType.Underground, direction: CardinalDirection) {
            val lit = otherBelt.selectedBelt[direction]?.get(type) ?: return
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

private fun Grid.constrainIsolatedUnderground(
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
        val otherBelt = belts[tile.shifted(ugDir, dist)] ?: continue
        val breaksPairThisTile = mutableListOf<Literal>()
        fun disallow(lit: Literal) {
            cp.addEquality(lit, false).apply {
                onlyEnforceIf(thisSelected)
                for (previous in breaksPair) onlyEnforceIf(!previous)
            }
        }
        for ((otherType, otherSelected) in otherBelt.selectedBelt[direction].orEmpty()) {
            if (thisType.prototype != otherType.prototype) continue
            if (thisType.hasInput == otherType.hasInput)
                breaksPairThisTile.add(otherSelected) // same direction, same type -- breaks pair
            else
                disallow(otherSelected) // would pair with this one
        }
        for ((otherType, otherSelected) in otherBelt.selectedBelt[direction.oppositeDir()].orEmpty()) {
            if (thisType.prototype != otherType.prototype) continue
            if (thisType.hasInput == otherType.hasInput) // would pair with this one
                disallow(otherSelected)
            else
                breaksPairThisTile.add(otherSelected) // same direction, same type -- breaks pair
        }
        breaksPair += breaksPairThisTile
    }
}

internal fun EntityPlacementModel.addBeltPlacements(grid: Grid) {
    for ((tile, belt) in grid.belts) {
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

fun EntityPlacementModel.addBeltGrid(grid: GridConfig): Grid {
    val vars = grid.applyTo(cp)
    addBeltPlacements(vars)
    return vars
}
