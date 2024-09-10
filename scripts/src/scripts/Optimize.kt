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
import kotlin.collections.mapOf

val projectRoot = File(".")

private val sourceFile = "blueprints/base-100-after-belts.txt"

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
        optimizeBeltLines {
            withCp = false
        }
        optimizePoles("small-electric-pole", "medium-electric-pole") {
            enforcePolesConnected = true
            addExistingAsInitialSolution = true
        }
        keepEntitiesWithCircuitConnections()
        keepIf {
            it.stage() <= 7
        }

        entityCosts = mapOf(
            "transport-belt" to 1.5,
            "underground-belt" to 17.5 / 2,
            "fast-transport-belt" to 11.5,
            "fast-underground-belt" to 97.5 / 2,
            "small-electric-pole" to 0.5,
            "medium-electric-pole" to 13.5,
        )
            .mapValues { it.value + 3.5 }
            .mapKeys { VanillaPrototypes[it.key] as EntityPrototype }
        distanceCostFactor = 5e-4
    }.build()
    model.solver.parameters.apply {
        maxTimeInSeconds = 60.0 * 15

//        maxPresolveIterations = 3

//        repairHint = true
//        hintConflictLimit = 500

//        stopAfterFirstSolution = true
//        numWorkers = 10
    }
    bp.entities.clear()

    val flow = model.solveFlow()
    flow
        .conflate()
        .transform {
            emit(it)
            delay(10_000)
        }.conflate()
        .withIndex()
        .onEach { (index, entities) ->
            bp.entities = model.exportFromSelectedOptionals(entities)
            val outFile = projectRoot.resolve("output-per/${fileName}-${"%03d".format(index)}.txt")
            outFile.parentFile.mkdirs()
            bp.toJson().exportTo(outFile)

            val numEntities = entities.size
            val infoFile = projectRoot.resolve("output-per/${fileName}-${"%03d".format(index)}.info.txt")
            infoFile.writeText("numEntities: $numEntities\n")
        }.launchIn(this)
}
