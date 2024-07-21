package scripts

import glassbricks.factorio.blueprint.Vec2d
import glassbricks.factorio.blueprint.entity.ElectricPole
import glassbricks.factorio.blueprint.entity.removeAllWithConnections
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprint.model.BlueprintModel
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.EntityPlacementOption
import glassbricks.factorio.blueprint.placement.addDistanceCostFrom
import glassbricks.factorio.blueprint.placement.poles.DistanceBasedConnectivity
import glassbricks.factorio.blueprint.placement.poles.PolePlacementOptions
import glassbricks.factorio.blueprint.placement.poles.createPolePlacements
import glassbricks.factorio.blueprint.placement.poles.euclidianDistancePlus
import glassbricks.factorio.blueprint.placement.toEntity
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

    val inputBp = projectRoot.resolve("test-blueprints/base8.txt")

    val bp = BlueprintModel(importBlueprint(inputBp) as BlueprintJson)
    val entities = bp.entities

    val originalPoles = entities.filterIsInstance<ElectricPole>()
    val polesCount = originalPoles.groupingBy { it.prototype }.eachCount()
    for ((pole, count) in polesCount) {
        println("${pole.name}: $count")
    }

    println("Setting up problem")
    val center = entities.enclosingBox().center()
    entities.removeAllWithConnections(
        originalPoles.filter { it.prototype == smallPole }
    )

    val model = EntityPlacementModel()
    model.addFixedEntities(entities)

    val polePlacements = model.createPolePlacements(
        listOf(
            smallPole,
//            mediumPole,
        ),
        entities.enclodingTileBox(),
        options = PolePlacementOptions(removeEmptyPolesReach1 = true)
    )
    polePlacements.poles.forEach {
        if (it is EntityPlacementOption<*>) {
            it.cost = if (it.prototype == smallPole) 1.0 else 4.0
        }
    }

    DistanceBasedConnectivity.fromExistingPolesOrPt(
        polePlacements,
        relativePos = Vec2d(0.5, 0.5),
        distanceMetric = euclidianDistancePlus(0.0),
    ).apply {
        addConstraints()
        launch {
            drawEntities(model.placements)
                .saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-candidate").absolutePath)
            drawingFor(model.placements).apply {
                drawEntities(model.placements.filter { it.isFixed })
                drawHeatmap(distances.distances)
                saveTo(projectRoot.resolve("output/${inputBp.nameWithoutExtension}-distances").absolutePath)
            }
        }
    }

    model.addDistanceCostFrom(center)

    println("Solving")
    model.timeLimitInSeconds = TIME_LIMIT
    val result = model.solve()
    println("Solved: ${result.status}")

    val selectedPoles = result.getSelectedOptionalEntities()
    val poleCounts =
        selectedPoles.groupingBy { it.prototype }.eachCount()
    for ((pole, count) in poleCounts) {
        println("${pole.name}: $count")
    }

    println("Saving result")

    for (pole in selectedPoles) {
        entities.add(pole.toEntity())
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
