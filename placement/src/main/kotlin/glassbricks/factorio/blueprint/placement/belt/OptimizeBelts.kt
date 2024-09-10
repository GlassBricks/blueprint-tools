package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.entity.TransportBelt
import glassbricks.factorio.blueprint.entity.UndergroundBelt
import glassbricks.factorio.blueprint.entity.copyEntitiesSpatial
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes

fun SpatialDataStructure<BlueprintEntity>.withOptimizedBeltLines(
    costs: BeltLineCosts,
    params: BeltLineSolveParams = BeltLineSolveParams(),
    prototypes: BlueprintPrototypes = VanillaPrototypes,
): MutableSpatialDataStructure<BlueprintEntity> {
    val entities = this.copyEntitiesSpatial()
    val result = entities
        .filterNotTo(mutableListOf()) { it is TransportBelt || it is UndergroundBelt }
        .let { DefaultSpatialDataStructure(it) }

    val grid = getBeltGrid(this, prototypes)
    val solution = solveBeltLines(grid, costs.notIntersecting(result), params)
    for (line in solution) {
        result.addAll(line.solutionEntities(entitiesToCopyFrom = entities))
    }
    verifySolution(entities, solution)
    return result
}
