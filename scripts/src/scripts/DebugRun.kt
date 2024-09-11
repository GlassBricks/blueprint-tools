package scripts

import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.json.importBlueprintString
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.BpModelBuilder
import glassbricks.factorio.blueprint.placement.PlacementSolution
import glassbricks.factorio.blueprint.prototypes.EntityPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
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
    val bp = tryImportFromClipboard() ?: Blueprint(importBlueprintFrom(File("blueprints/base-100-belts2.txt")))
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
//    export("no-cp", tryOpt(result, withCp = false).export())
//    export("no-force", tryOpt(result, force = false).export())
}

private fun tryOpt(
    entities: SpatialDataStructure<BlueprintEntity>,
    withCp: Boolean = true,
    force: Boolean = true,
): PlacementSolution {
    val model = BpModelBuilder(entities).apply {
//        optimizeBeltLines {
//            this.withCp = withCp
////            this.forceWithCp = force
//        }
        optimizePoles("small-electric-pole", "medium-electric-pole") {
            enforcePolesConnected = true
            ignoreUnpoweredEntities = true
//            addExistingAsInitialSolution = true
        }
        addSafeNudging()
        entityCosts = mapOf(
            "transport-belt" to 5.0,
            "underground-belt" to 5.0 * 4.8 / 2,
            "fast-transport-belt" to 15.0,
            "fast-underground-belt" to 15.0 * 5.8 / 2,
            "small-electric-pole" to 4.0,
            "medium-electric-pole" to 15.0
        )
            .mapKeys { VanillaPrototypes[it.key] as EntityPrototype }
        keepEntitiesWithCircuitConnections()
        keepIf {
            it.stage() <= 7
        }
    }.build()
    model.solver.parameters.apply {
        maxTimeInSeconds = 60.0

//            repairHint = true
//            hintConflictLimit = 100

    }
    return model.solve(optimize = false)
}
