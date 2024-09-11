package scripts

import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.json.importBlueprintString
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.mapOf

val projectRoot = File(".")

private val sourceFile = "blueprints/base-100-belts2.txt"

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

    val model = BpModelBuilder(bp).apply {
//        optimizeBeltLines {
//            withCp = true
//        }
        optimizePoles("small-electric-pole", "medium-electric-pole") {
            enforcePolesConnected = true
            addExistingAsInitialSolution = true
        }
//        addSafeNudging()
        keepEntitiesWithCircuitConnections()
        keepIf {
            it.stage() <= 13
        }

        entityCosts = mapOf(
            "transport-belt" to 5.0,
            "underground-belt" to 5.0 * 4.8 / 2,
            "fast-transport-belt" to 15.0,
            "fast-underground-belt" to 15.0 * 5.8 / 2,
            "small-electric-pole" to 4.0,
            "medium-electric-pole" to 15.0
        )
            .mapKeys { VanillaPrototypes[it.key] as EntityPrototype }
        distanceCostFactor = 5e-4
    }.build()
    model.solver.parameters.apply {
        maxTimeInSeconds = 60.0 * 30

//        maxPresolveIterations = 5

//        repairHint = true
//        hintConflictLimit = 500

//        stopAfterFirstSolution = true
//        numWorkers = 10
    }

    val flow = model.solveFlow()
    flow
        .conflate()
        .transform {
            emit(it)
            delay(1_000)
        }.conflate()
        .withIndex()
        .onEach { (index, entities) ->
            bp.entities = model.exportFromSelectedOptionals(entities)
            val outFile = projectRoot.resolve("output-per/${fileName}-2-${"%03d".format(index)}.txt")
            outFile.parentFile.mkdirs()
            bp.toJson().exportTo(outFile)

            val infoFile = projectRoot.resolve("output-per/${fileName}.info.txt")
            val toCount = listOf(
                "transport-belt",
                "underground-belt",
                "fast-transport-belt",
                "fast-underground-belt",
                "small-electric-pole",
                "medium-electric-pole",
            )
            val writer = FileOutputStream(infoFile, true).bufferedWriter()
            writer.appendLine("#$index")
            for (entity in toCount) {
                val count = bp.entities.count { it.name == entity }
                writer.appendLine("$entity: $count")
            }
            writer.appendLine()
            writer.close()
        }.launchIn(this)
}
