import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.removeWithConnectionsIf
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.poleopt.createDefaultPoleILP
import glassbricks.factorio.blueprint.poleopt.getCompleteCandidatePoleSet
import java.io.File


val removeAllPoles = true
val candidatePrototypes = listOf(smallPole)

fun main(args: Array<String>) {
    val file = File(args.getOrNull(0) ?: "test-blueprints/base8.txt")
    val bp = BlueprintModel(importBlueprint(file) as BlueprintJson)

    val entities = bp.entities

    val originalPoles = entities.filterIsInstance<ElectricPole>()
    val polesCount = originalPoles.groupingBy { it.prototype }.eachCount()
    for ((pole, count) in polesCount) {
        println("${pole.name}: $count")
    }

    if (removeAllPoles) {
        entities.removeWithConnectionsIf { it is ElectricPole }
    }

    println("Getting candidate poles...")
    val candidatePoles = getCompleteCandidatePoleSet(entities, candidatePrototypes, entities.enclosingBox())

    println("Candidate poles: ${candidatePoles.poles.size}")


    println("Creating pole problem...")
    val poleProblem = createDefaultPoleILP(candidatePoles)
    poleProblem.solver.setTimeLimit(30_000)

    println("Solving...")
    val result = poleProblem.solver.solve()
    println(result)

    println("Objective: ${poleProblem.solver.objective().value()}")
}
