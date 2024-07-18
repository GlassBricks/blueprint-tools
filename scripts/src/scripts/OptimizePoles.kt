package scripts

import glassbricks.factorio.blueprint.Vec2d
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.copyEntities
import glassbricks.factorio.blueprint.entity.removeAllWithConnections
import glassbricks.factorio.blueprint.entity.removeWithConnectionsIf
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.poleopt.*
import glassbricks.factorio.scripts.drawEntities
import glassbricks.factorio.scripts.drawHeatmap
import glassbricks.factorio.scripts.smallPole
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

suspend fun main() = coroutineScope {
    val projectRoot = File(".")

    val inputBp = projectRoot.resolve("test-blueprints/base8.txt")

    val bp = BlueprintModel(importBlueprint(inputBp) as BlueprintJson)
    val entities = bp.entities

    val originalPoles = entities.filterIsInstance<ElectricPole>()
    val polesCount = originalPoles.groupingBy { it.prototype }.eachCount()
    for ((pole, count) in polesCount) {
        println("${pole.name}: $count")
    }

    println("Setting up problem")
    entities.removeAllWithConnections(originalPoles.filter { it.prototype == smallPole })

    val problem = createPoleCoverProblem(
        entities.copyEntities(),
        polesToAdd = listOf(smallPole),
        entities.enclosingBox(),
        forceIncludeExistingPoles = true
    ).apply {
        removeEmptyPolesDegree2()
    }
    launch {
        drawEntities(problem.candidatePoles)
            .drawEntities(problem.entities)
            .saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-candidate").absolutePath)
    }

    val ilp = defaultPoleCoverILPSolver(problem).apply {
        DistanceBasedConnectivity.fromAroundPt(this, Vec2d(0.5, 0.5)).apply {
            addConstraints()

            drawEntities(problem.entities).apply {
                drawHeatmap(distances)
            }.saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-distances").absolutePath)
        }
    }


    println("Solving ilp")
    ilp.solver.setTimeLimit(30_000)
    val result = ilp.solve()
    println(result)

    val poleCounts = ilp.poleVariables.filter { it.value.solutionValue() }
        .keys.groupingBy { it.prototype }.eachCount()
    for ((pole, count) in poleCounts) {
        println("${pole.name}: $count")
    }

    entities.removeWithConnectionsIf { it is ElectricPole }
    for (pole in ilp.getSelectedPoles()) {
        entities.add(pole.toEntity())
    }

    drawEntities(entities)
        .saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-result").absolutePath)

    val outFile = projectRoot.resolve("output/${inputBp.name}")
    outFile.parentFile.mkdirs()
    bp.toBlueprint().exportTo(outFile)


}
