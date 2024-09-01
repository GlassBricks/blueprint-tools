package glassbricks.factorio

import glassbricks.factorio.blueprint.json.bpJson
import glassbricks.factorio.blueprint.json.importBlueprintJson
import glassbricks.factorio.blueprint.json.importBlueprintString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private val bpJsonPretty = Json(bpJson) {
    prettyPrint = true
}

fun main() {
    while (true) {
        println("Input str:")
        val str = readlnOrNull() ?: break
        val bp = try {
            importBlueprintString(str)
        } catch (e: Exception) {
            println("As simple json")
            try {
                val backToString = importBlueprintJson(str.byteInputStream(), JsonElement.serializer())
                val pretty = bpJsonPretty.encodeToString(backToString)
                println(pretty)
            } catch (e: Exception) {
                println("Error: $e")
            }
            continue
        }
        println("Blueprint: $bp")

        val backToString = bpJsonPretty.encodeToString(bp)
        println("Json string: $backToString")
    }
}
