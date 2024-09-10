package scripts

import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.model.Blueprint
import java.io.File

fun main() {
    val bp = Blueprint(importBlueprintFrom(File("output-per/base-100-after-belts-033.txt")))
    bp.entities.forEach {
        if (it.stage() > 7) {
            it.tags = null
        }
    }
    val outFile = File("output/base-100-remstage.txt")
    bp.toJson().exportTo(outFile)
}
