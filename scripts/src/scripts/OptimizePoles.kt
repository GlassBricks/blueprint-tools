package scripts

import glassbricks.factorio.blueprint.Vector
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.removeAllWithConnections
import glassbricks.factorio.blueprint.entity.removeWithConnectionsIf
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.placement.*
import glassbricks.factorio.blueprint.placement.poles.DistanceBasedConnectivity
import glassbricks.factorio.blueprint.placement.poles.PolePlacementOptions
import glassbricks.factorio.blueprint.placement.poles.addPolePlacements
import glassbricks.factorio.blueprint.placement.poles.euclidianDistancePlus
import glassbricks.factorio.blueprint.pos
import glassbricks.factorio.blueprint.prototypes.FurnacePrototype
import glassbricks.factorio.blueprint.prototypes.InserterPrototype
import glassbricks.factorio.scripts.drawEntities
import glassbricks.factorio.scripts.drawHeatmap
import glassbricks.factorio.scripts.drawingFor
import glassbricks.factorio.scripts.smallPole
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File

// in seconds
private const val TIME_LIMIT = 90.0

val logger = KotlinLogging.logger {}

suspend fun main(): Unit = coroutineScope {
    val projectRoot = File(".")

    val inputBp = projectRoot.resolve("blueprints/triplegc.txt")

    val bp = BlueprintModel(importBlueprint(inputBp) as BlueprintJson)
    val entities = bp.entities

    val originalPoles = entities.filterIsInstance<ElectricPole>()
    entities.removeAllWithConnections(originalPoles)
//    val polesCount = originalPoles.groupingBy { it.prototype }.eachCount()
//    for ((pole, count) in polesCount) {
//        println("${pole.name}: $count")
//    }
//    val center = entities.enclosingBox().center()

    println("Setting up problem")

    val model = EntityPlacementModel()
    model.addFixedEntities(entities)

    val canNudge = model.placements.filterTo(mutableSetOf()) {
        it.prototype is InserterPrototype
    }
    model.addEntityNudgingWithInserters(canNudge, nudgeCost = 0.0)

    // todo: make this nicer
    entities.removeWithConnectionsIf {
        it.prototype is InserterPrototype
    }

    val polePlacements = model.addPolePlacements(
        listOf(smallPole),
        entities.enclodingTileBox(),
        options = PolePlacementOptions(removeEmptyPolesReach1 = true)
    )
    polePlacements.poles.forEach {
        if (it is OptionalEntityPlacement<*>) {
            it.cost = if (it.prototype == smallPole) 1.0 else 4.0
        }
    }
    drawEntities(model.placements)
        .saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-candidate").absolutePath)

    DistanceBasedConnectivity.fromExistingPolesOrPt(
        polePlacements,
        relativePos = Vector(0.5, 0.5),
        distanceMetric = euclidianDistancePlus(3.0),
    ).apply {
        addConstraints()
        launch {
            drawingFor(model.placements).apply {
                drawEntities(model.placements.filter { it.isFixed })
                drawHeatmap(distances.distances)
                saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-distances").absolutePath)
            }
        }
    }

    val bottom = entities.enclosingBox().let {
        pos((it.minX + it.maxX) / 2, it.maxY)
    }
    model.addDistanceCostFrom(bottom)

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

    println("Saving result")

    for (entity in selectedEntities) {
        entities.add(entity.toEntity())
    }

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