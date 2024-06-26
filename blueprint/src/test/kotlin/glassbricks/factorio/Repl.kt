package glassbricks.factorio

import glassbricks.factorio.blueprint.bpJson
import glassbricks.factorio.blueprint.importBlueprint
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
            importBlueprint(str)
        } catch (e: Exception) {
            println("As simple json")
            try {
                val backToString = importBlueprint(str, JsonElement.serializer())
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
