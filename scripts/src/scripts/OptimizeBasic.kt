package scripts

import drawing.drawEntities
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintJson
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.mapOf
import kotlin.system.exitProcess

val projectRoot = File(".")

suspend fun main(): Unit = coroutineScope {
    val inputFile = projectRoot.resolve("scripts/test-blueprints/earlybase.txt")
    val bp = Blueprint(importBlueprintJson(inputFile))
    val model = BpModelBuilder(bp).apply {
        optimizeBeltLines = true
        optimizePoles = listOf(
            VanillaPrototypes.getAs("small-electric-pole"),
            VanillaPrototypes.getAs("medium-electric-pole"),
        )
        enforcePolesConnected = true

        setEntityCosts(mapOf(
            "transport-belt" to 1.5,
            "underground-belt" to 17.5 / 2,
            "fast-transport-belt" to 11.5,
            "fast-underground-belt" to 97.5 / 2,
            "small-electric-pole" to 0.5,
            "medium-electric-pole" to 13.5,
        ).mapValues { it.value + 3.4 })
//        check(entityCosts["underground-belt"]!! * 3 < entityCosts["transport-belt"]!! * 5)
//        check(entityCosts["underground-belt"]!! * 3 > entityCosts["transport-belt"]!! * 4)

        distanceCostFactor = 1e-3

        keepWithControlBehavior()
    }.build()
    model.timeLimitInSeconds = 60.0 * 10
    model.solver.parameters.apply {
        useObjectiveLbSearch = true
        maxMemoryInMb = 1024 * 8
    }

    val fileName = inputFile.nameWithoutExtension
    drawEntities(model.placements).saveTo("output/${fileName}-pre-solve")

    val solution = model.solve()
    println("Solved: ${solution.status}")
    if (!solution.isOk) exitProcess(1)

    val entities = solution.export()

    val entityCounts =
        entities.groupingBy { it.prototype.name }.eachCount().entries.sortedByDescending { it.value }.take(20)
    for ((name, count) in entityCounts) {
        println("$name: $count")
    }

    println("Saving result")

    bp.entities.clear()
    bp.entities.addAll(entities)

    launch {
        val outFile = projectRoot.resolve("output/${fileName}-result.txt")
        outFile.parentFile.mkdirs()
        bp.toJson().exportTo(outFile)
    }
    launch {
        drawEntities(entities).saveTo(projectRoot.resolve("output/${fileName}-result.png").absolutePath)
    }
}
