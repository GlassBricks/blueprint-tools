package scripts

import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.json.importBlueprintString
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.BpModelBuilder
import java.io.File

fun tryImportFromClipboard(): Blueprint? {
    return null
    return try {
        Blueprint(importBlueprintString(getClipboard()))
            .also { println("Imported from clipboard") }
    } catch (_: Exception) {
        null
    }
}

fun main() {
    val bp = tryImportFromClipboard() ?: Blueprint(importBlueprintFrom(File("blueprints/base-100-iron.txt")))
    val result = bisectBp(bp.entities) { entities ->
//        entities.withOptimizedBeltLines(BeltLineCosts { _, _, _ -> 1.0 })
        val model = BpModelBuilder(bp).apply {
            optimizeBeltLines {
                withCp = true
            }
            keepEntitiesWithControlBehavior()
        }.build()
        model.solver.parameters.apply {
            maxTimeInSeconds = 60.0

            repairHint = true
            hintConflictLimit = 100

            stopAfterFirstSolution = true
        }
        val result = model.solve()
        check(result.isOk)
    }


    val newBp = Blueprint()
    newBp.entities.addAll(result)

    val outFile = projectRoot.resolve("output/debug.txt")
    outFile.parentFile.mkdirs()
    newBp.toJson().exportTo(outFile)
    println("Exported to $outFile")
    println("Has: ${newBp.entities.size} entities")
}
