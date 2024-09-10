package scripts

import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.json.importBlueprintString
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.BpModelBuilder
import glassbricks.factorio.blueprint.placement.PlacementSolution
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
        val result = tryOpt(entities)
        check(result.isOk)
    }


    fun export(name: String, entities: Collection<BlueprintEntity>) {
        val bp = Blueprint()
        bp.entities.addAll(entities)
        val outFile = projectRoot.resolve("debug-out/$name.txt")
        bp.toJson().exportTo(outFile)
        println("Exported to $outFile")
    }
    export("problem", result)
    export("no-cp", tryOpt(result, withCp = false).export())
    export("no-force", tryOpt(result, force = false).export())
}

private fun tryOpt(
    entities: SpatialDataStructure<BlueprintEntity>,
    withCp: Boolean = true,
    force: Boolean = true,
): PlacementSolution {
    val model = BpModelBuilder(entities).apply {
        optimizeBeltLines {
            this.withCp = withCp
            forceWithCp = force
        }
        setEntityCosts(
            "transport-belt" to 1.0,
            "underground-belt" to 3.4,
            "fast-transport-belt" to 5.0,
            "fast-underground-belt" to 19.0,
        )
        keepEntitiesWithCircuitConnections()
    }.build()
    model.solver.parameters.apply {
        maxTimeInSeconds = 60.0

//            repairHint = true
//            hintConflictLimit = 100

        stopAfterFirstSolution = true
    }
    val result = model.solve(display = false)
    return result
}
