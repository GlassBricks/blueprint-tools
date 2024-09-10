package scripts

import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.model.Blueprint
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

fun main() {
    val bp = Blueprint(importBlueprintFrom(File("output/base-100-belt-cp.txt")))
    println(bp.entities.take(5).map { it.tags })
    val stageEntities = bp.entities.groupingBy { it.stage() }.eachCount().toSortedMap()
    println(stageEntities)
}

fun BlueprintEntity.stage(): Int {
    return tags?.get("bp100")?.jsonObject?.get("firstStage")?.jsonPrimitive?.int ?: 0
}
