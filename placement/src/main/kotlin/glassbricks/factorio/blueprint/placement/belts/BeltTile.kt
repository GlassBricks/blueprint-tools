package glassbricks.factorio.blueprint.placement.belts

import glassbricks.factorio.blueprint.placement.Disjunction
import glassbricks.factorio.blueprint.placement.Literal
import glassbricks.factorio.blueprint.placement.WithCp
import glassbricks.factorio.blueprint.placement.any
import glassbricks.factorio.blueprint.placement.belts.BeltType.*

enum class BeltType {
    Empty,
    Belt,
    InputUnderground,
    OutputUnderground
}

interface BeltTileVars : WithCp {

    val isEmpty: Literal
    val isBelt: Literal

    val isInputUndergroundIn: PerDirectionVars
    val isOutputUndergroundIn: PerDirectionVars

    val isInputUnderground: Disjunction
    val isOutputUnderground: Disjunction

    val inputDirection: PerDirectionVars
    val outputDirection: PerDirectionVars

    /**
     * "virtual" underground belts that connect an input and output underground belt
     */
    val hasUndergroundConnection: PerDirectionVars
}

fun BeltTileVars.isType(type: BeltType): Disjunction = when (type) {
    Empty -> isEmpty
    Belt -> isBelt
    InputUnderground -> any(isInputUnderground)
    OutputUnderground -> any(isOutputUnderground)
}
