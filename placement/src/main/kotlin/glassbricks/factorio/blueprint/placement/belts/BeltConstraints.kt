package glassbricks.factorio.blueprint.placement.belts

import glassbricks.factorio.blueprint.placement.any
import glassbricks.factorio.blueprint.placement.none

@Suppress("unused")
private fun handledType() {
    val type: BeltType = null!!
    when (type) {
        BeltType.Empty -> {}
        BeltType.Belt -> {}
        BeltType.InputUnderground -> {}
        BeltType.OutputUnderground -> {}
    }
}

fun addBasicTileConstraints(tile: BeltTileVars, type: BeltTileType) {
    tile.cp.addExactlyOne(
        listOf(
            listOf(tile.isEmpty, tile.isBelt),
            tile.isInputUndergroundIn,
            tile.isOutputUndergroundIn,
        ).flatten()
    )
    tile.constrainInOut(type)
    tile.constrainUndergroundVars()
}

private fun BeltTileVars.constrainInOut(type: BeltTileType) {
    cp.addAtMostOne(mainInputDirection)
    cp.addAtMostOne(outputDirection)
    isEmpty.implies(none(mainInputDirection + outputDirection))

    when (type) {
        BeltTileType.ConnectingBelt -> {
            isBelt.impliesAll(
                any(mainInputDirection),
                any(outputDirection)
            )
            for (direction in CardinalDirection.entries) {
                isInputUndergroundIn[direction].impliesAll(
                    mainInputDirection[direction],
                    none(outputDirection)
                )
                isOutputUndergroundIn[direction].impliesAll(
                    outputDirection[direction],
                    none(mainInputDirection)
                )
            }
        }

        BeltTileType.FixedInputBelt -> setAllTrue(
            isBelt,
            none(mainInputDirection),
            any(outputDirection)
        )

        BeltTileType.FixedOutputSpot -> setAllTrue(
            isBelt,
            any(mainInputDirection),
            none(outputDirection)
        )
    }
}

private fun BeltTileVars.constrainUndergroundVars() {
    // for each axis, only one direction of ug connections can be used
    isUndergroundConnection.let {
        cp.addAtMostOne(listOf(it[CardinalDirection.North], it[CardinalDirection.South]))
        cp.addAtMostOne(listOf(it[CardinalDirection.East], it[CardinalDirection.West]))
    }
    // if is input or output underground, cannot have underground connection in same axis
    for (direction in CardinalDirection.entries) {
        any(
            isInputUndergroundIn[direction],
            isOutputUndergroundIn[direction]
        ).implies(
            none(
                isUndergroundConnection[direction],
                isUndergroundConnection[direction.oppositeDir()]
            )
        )
    }
}

internal fun BeltGrid.enforceInOut() {
    for ((pos, tile) in tiles) for (direction in CardinalDirection.entries) {
        val forwardTile = getTileShifted(pos, direction) ?: emptyTile
        // todo: handle sideloading here
        cp.addEquality(tile.outputDirection[direction], forwardTile.mainInputDirection[direction])
    }
}

internal fun BeltGrid.enforceUndergroundConnections() {
    for ((pos, tile) in tiles) for (direction in CardinalDirection.entries) {
        val forwardTile = getTileShifted(pos, direction) ?: emptyTile
        any(
            tile.isInputUndergroundIn[direction],
            tile.isUndergroundConnection[direction]
        ).implies(
            any(
                forwardTile.isOutputUndergroundIn[direction],
                forwardTile.isUndergroundConnection[direction]
            )
        )
        val backwardTile = getTileShifted(pos, direction.oppositeDir()) ?: emptyTile
        any(
            tile.isOutputUndergroundIn[direction],
            tile.isUndergroundConnection[direction]
        ).implies(
            any(
                backwardTile.isInputUndergroundIn[direction],
                backwardTile.isUndergroundConnection[direction]
            )
        )
    }
}

internal fun BeltGrid.enforceUndergroundMaxLength() {
    for ((pos, tile) in tiles) for (direction in CardinalDirection.entries) {
        val outUndergrounds = (1..undergroundMaxLength).mapNotNull {
            getTileShifted(pos, direction, it)?.isOutputUndergroundIn?.get(direction)
        }
        tile.isInputUndergroundIn[direction].implies(any(outUndergrounds))
    }
}
