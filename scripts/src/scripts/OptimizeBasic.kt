package scripts

import glassbricks.factorio.blueprint.Entity
import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.removeAllWithConnections
import glassbricks.factorio.blueprint.entity.removeWithConnectionsIf
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.placement.ops.BeltCosts
import glassbricks.factorio.blueprint.placement.ops.addEntityNudgingWithInserters
import glassbricks.factorio.blueprint.placement.ops.optimizeBeltLinesInBp
import glassbricks.factorio.blueprint.placement.poles.*
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import glassbricks.factorio.scripts.drawEntities
import glassbricks.factorio.scripts.drawHeatmap
import glassbricks.factorio.scripts.drawingFor
import glassbricks.factorio.scripts.smallPole
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

// in seconds
private const val TIME_LIMIT = 60.0

val projectRoot = File(".")
private val inputBp = projectRoot.resolve("blueprints/outpost5.txt")

val optimizeBelts: BeltCosts? = null

//BeltCosts(
//    mapOf(
//        VanillaPrototypes["transport-belt"]!! to 1.0,
//        VanillaPrototypes["underground-belt"]!! to 2.4,
//    ),
//    overlapCost = 1.0,
//)
val nudgeInserters = false

val optimizePoles = true
val poleConnectivity: PoleConnectivity = PoleConnectivity.Label
val addDistanceCost = true

enum class PoleConnectivity { Distance, Label }

suspend fun main(): Unit = coroutineScope {
    val bp = BlueprintModel(importBlueprint(inputBp) as BlueprintJson)
    val entities = bp.entities

    if (optimizeBelts != null) {
        println("Optimizing belts")

        if (addDistanceCost) {
            val center = entities.enclosingBox().center()
            optimizeBelts.additionalCostFn = { _, pos, _ -> pos.center().distanceTo(center) / 1e8 }
        }
        optimizeBeltLinesInBp(entities, optimizeBelts)
    }

    if (optimizePoles) {
        println("Optimizing poles: setting up problem")
        val originalPoles = entities.filterIsInstance<ElectricPole>()
        entities.removeAllWithConnections(originalPoles)


        val model = EntityPlacementModel()
        model.addFixedEntities(entities)

        if (nudgeInserters) {
            val canNudge = model.placements.filterTo(mutableSetOf()) { it.prototype is InserterPrototype }
            model.addEntityNudgingWithInserters(canNudge, nudgeCost = 0.0)
            entities.removeWithConnectionsIf { it.prototype is InserterPrototype }
        }

        val allPoles: MutableList<Entity<ElectricPolePrototype>> =
            model.getAllPossibleUnrotatedPlacements(listOf(smallPole), entities.enclosingTileBox().expand(1))
                .toMutableList()
        for (pole in allPoles) {
            model.addPlacement(pole)
        }
        val polePlacements = PolePlacements(model, PolePlacementOptions(removeEmptyPolesReach1 = true))
        polePlacements.poles.forEach {
            if (it is OptionalEntityPlacement<*>) {
                it.cost = if (it.prototype == smallPole) 1.0 else 4.0
            }
        }
        drawEntities(model.placements)
            .saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-candidate").absolutePath)

        when (poleConnectivity) {
            null -> {}
            PoleConnectivity.Distance -> DistanceDAGConnectivity(
                polePlacements,
                rootPoles = polePlacements.rootPolesFromExistingOrNear(Vector(0.5, 0.8)),
                distanceMetric = favorPolesThatPowerMore(polePlacements)
            ).apply {
                addConstraints()
                launch {
                    drawingFor(model.placements).apply {
                        drawEntities(model.placements.filter { it.isFixed })
                        drawHeatmap(distances)
                        saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-distances").absolutePath)
                    }
                }
            }

            PoleConnectivity.Label -> DistanceLabelConnectivity(
                polePlacements,
                rootPoles = polePlacements.rootPolesFromExistingOrNear(Vector(0.5, 0.5)),
            ).addConstraints()
        }

        if (addDistanceCost)
            model.addDistanceCostFrom(entities.enclosingBox().center())

        println("Solving")
        model.timeLimitInSeconds = TIME_LIMIT
        val result = model.solve()
        println("Solved: ${result.status}")

        val selectedEntities = result.getSelectedOptionalEntities()
        val poleCounts =
            selectedEntities.groupingBy { it.prototype }.eachCount()
        for ((pole, count) in poleCounts) {
            println("${pole.name}: $count")
        }
        for (entity in selectedEntities) {
            entities.add(entity.toEntity())
        }
    }

    println("Saving result")

    launch {
        val outFile = projectRoot.resolve("output/${inputBp.name}")
        outFile.parentFile.mkdirs()
        bp.toBlueprint().exportTo(outFile)
    }
    launch {
        drawEntities(entities)
            .saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-result").absolutePath)
    }
}
