package scripts

import drawing.drawEntities
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.json.importBlueprintString
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlin.collections.mapOf
import kotlin.system.exitProcess

val projectRoot = File(".")

private val sourceFile = "blueprints/base-100-iron.txt"

suspend fun main(): Unit = coroutineScope {
    println("importing blueprint")
    val bp: Blueprint
    val fileName: String
    if (sourceFile.isNotEmpty()) {
        bp = Blueprint(importBlueprintFrom(projectRoot.resolve(sourceFile)))
        fileName = projectRoot.resolve(sourceFile).nameWithoutExtension
    } else {
        val clipboardContents = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        bp = Blueprint(importBlueprintString(clipboardContents))
        fileName = "clipboard"
    }

    val entityCosts = mapOf(
        "transport-belt" to 1.5,
        "underground-belt" to 17.5 / 2,
        "fast-transport-belt" to 11.5,
        "fast-underground-belt" to 97.5 / 2,
        "small-electric-pole" to 0.5,
        "medium-electric-pole" to 13.5,
    )
        .mapValues { it.value + 3.5 }
        .mapKeys { VanillaPrototypes[it.key] as EntityPrototype }

    val model = BpModelBuilder(bp).apply {
//        optimizePoles("small-electric-pole", "medium-electric-pole") {
//            enforcePolesConnected = true
//            addExistingAsInitialSolution = true
//        }
        optimizeBeltLines {
            withCp = true
            forceWithCp = true
        }
        keepEntitiesWithControlBehavior()
        keepIf {
            it.stage() <= 7
        }

        this.entityCosts = entityCosts

        distanceCostFactor = 5e-4
    }.build()
    model.solver.parameters.apply {
        maxTimeInSeconds = 60.0 * 15

//        maxPresolveIterations = 3

//        repairHint = true
//        hintConflictLimit = 500

//        stopAfterFirstSolution = true
//        numWorkers = 16
    }
    bp.entities.clear()

    val solution = model.solve()
    if (!solution.isOk) {
        println("Failed to find a solution: ${solution.status}")
        exitProcess(1)
    }

    val entities = solution.export()

    val entityCounts =
        entities.groupingBy { it.prototype.name }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10)
    for ((name, count) in entityCounts) {
        println("$name: $count")
    }

    println("Saving result")

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
