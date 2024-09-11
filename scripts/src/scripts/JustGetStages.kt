package scripts

import glassbricks.factorio.blueprint.entity.BlueprintEntity
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun main() {
//    val bp = Blueprint(importBlueprintFrom(File("blueprints/base-100-belts2.txt")))
    val bp = tryImportFromClipboard()!!
    println(bp.entities.take(5).map { it.tags })
    val stageEntities = bp.entities.groupingBy { it.stage() }.eachCount().toSortedMap()
    println(stageEntities)
}

fun BlueprintEntity.stage(): Int {
    return tags?.get("bp100")?.jsonObject?.get("firstStage")?.jsonPrimitive?.int ?: 1000
}
