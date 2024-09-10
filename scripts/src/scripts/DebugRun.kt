package scripts

import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.json.importBlueprintString
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.*
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
    val bp = tryImportFromClipboard() ?: Blueprint(importBlueprintFrom(File("test-blueprints/base8.txt")))
    val result = bisectBp(bp.entities) { entities ->
        BpModelBuilder(entities).apply {
            optimizeBeltLines()
            keepEntitiesWithControlBehavior()
        }.build()
    }


    val newBp = Blueprint()
    newBp.entities.addAll(result)

    val outFile = projectRoot.resolve("output/debug.txt")
    outFile.parentFile.mkdirs()
    newBp.toJson().exportTo(outFile)
    println("Exported to $outFile")
}
