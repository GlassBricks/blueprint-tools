package glassbricks.factorio.blueprint.placement.belts

import glassbricks.factorio.blueprint.placement.any
import glassbricks.factorio.blueprint.placement.implies
import glassbricks.factorio.blueprint.placement.impliesAll
import glassbricks.factorio.blueprint.placement.none

fun BeltTileVars.addBasicTileConstraints() {
    cp.addExactlyOne(BeltType.entries.flatMap { isType(it).literals })
    constrainInOut()
    constrainUndergroundVars()
}

internal fun BeltTileVars.constrainInOut() {
    isEmpty.implies(none(inputDirection + outputDirection))
    isBelt.impliesAll(
        any(inputDirection),
        any(outputDirection)
    )
    for (direction in CardinalDirection.entries) {
        isInputUndergroundIn[direction].impliesAll(
            inputDirection[direction],
            none(outputDirection)
        )
        isOutputUndergroundIn[direction].impliesAll(
            outputDirection[direction],
            none(inputDirection)
        )
    }
}

internal fun BeltTileVars.constrainUndergroundVars() {
    // for each axis, only one direction of ug connections can be used
    hasUndergroundConnection.let {
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
                hasUndergroundConnection[direction],
                hasUndergroundConnection[direction.oppositeDir()]
            )
        )
    }
    TODO("Complete this")
}
