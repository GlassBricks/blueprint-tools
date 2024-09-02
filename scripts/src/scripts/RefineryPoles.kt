package scripts

import drawing.drawEntities
import drawing.smallPole
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintJson
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.addDistanceCostFrom
import glassbricks.factorio.blueprint.placement.poles.*
import glassbricks.factorio.blueprint.placement.toBlueprintEntity
import kotlinx.coroutines.coroutineScope
import java.io.File


private val inputBp = projectRoot.resolve("blueprints/refineries.txt")


fun tiledCopiesX(
    entities: Collection<BlueprintEntity>,
    tileSize: Int,
    numTiles: Int,
): MutableSpatialDataStructure<BlueprintEntity> = DefaultSpatialDataStructure<BlueprintEntity>().apply {
    for (i in 0..<numTiles) for (entity in entities) {
        val copy = entity.copyIsolated()
        copy.position += pos(i * tileSize.toDouble(), 0.0)
        add(copy)
    }
}

suspend fun main(): Unit = coroutineScope {
    val bp = Blueprint(importBlueprintJson(inputBp) as BlueprintJson)
    val origEntities = bp.entities.filter { it !is ElectricPole }
    bp.entities.clear()
    val originalBox = getEnclosingBox(origEntities.map { it.collisionBox }).roundOutToTileBbox()
    val leftTop = originalBox.leftTop
        .add(2, 0)
        .topLeftCorner()
    for (entity in origEntities) {
        entity.position -= leftTop
    }

    val results = (1..4).associateWith { numTiles ->
        val xWrapSize = 8 * numTiles
        val thisEntities = tiledCopiesX(origEntities, 8, numTiles)

        val model = EntityPlacementModel(
            WrappingTileHashMap(tilePos(xWrapSize, 1000))
        )
        model.addFixedEntities(thisEntities)

        val polePlacements = model.addPolePlacements(
            listOf(smallPole),
            TileBoundingBox(TilePosition.ZERO, TilePosition(xWrapSize, originalBox.height)),
        ) {
            removeEmptyPolesDist2()
        }
        drawEntities(model.placements).saveTo(File("output/refineries-$numTiles-candidates").absolutePath)


        val rightPoint = pos(xWrapSize - 1.0, originalBox.height / 4.0)
        val rootPoles = polePlacements.getRootPolesNear(rightPoint)
        polePlacements.enforceConnectedWithDag(rootPoles)

        run {
            val poles = polePlacements.poles
            for (rootPole in rootPoles) {
                val wrappingPoles = poles.filter {
                    val xDist = (it.position.x + xWrapSize) - rootPole.position.x
                    val yDist = it.position.y - rootPole.position.y
                    val d = xDist * xDist + yDist * yDist
                    it != rootPole &&
                            d <= smallPole.maximum_wire_distance * smallPole.maximum_wire_distance
                }

                model.cp.addAtLeastOne(
                    wrappingPoles.map { it.placement.selected }
                ).onlyEnforceIf(rootPole.placement.selected)
            }
        }

        model.addDistanceCostFrom(rightPoint)

        model.timeLimitInSeconds = 60.0
        println("Running for $numTiles tiles")
        val solution = model.solve()
        println(solution.status)
        if (!solution.isOk) {
            println("No solution found")
            return@associateWith Double.POSITIVE_INFINITY
        }

        val result = bp.deepCopy()
        result.entities.apply {
            clear()
            addAll(thisEntities)
            for (pole in solution.getSelectedOptionalEntities()) {
                add(pole.toBlueprintEntity())
            }
        }

        drawEntities(result.entities).saveTo(File("output/refineries-$numTiles-result").absolutePath)

        result.toJson().exportTo(File("output/refineries-$numTiles-result.txt"))

        val numPoles = result.entities.count { it is ElectricPole }
        val cost = numPoles / numTiles.toDouble()

        cost
    }
    for ((numTiles, cost) in results.entries.sortedBy { it.value }) {
        println("$numTiles: $cost")
    }
}
