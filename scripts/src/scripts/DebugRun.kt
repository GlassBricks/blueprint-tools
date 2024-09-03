package scripts

import glassbricks.factorio.blueprint.json.exportTo
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.placement.*
import java.io.File


fun main() {
//    val clipboardContents = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
    val bp = Blueprint(importBlueprintFrom(File("test-blueprints/early-base.txt")))
    val result = bisectBp(bp.entities) { entities ->
        BpModelBuilder(entities).apply {
            optimizeBeltLines = true
            keepWithControlBehavior()
        }.build()
    }


    val newBp = Blueprint()
    newBp.entities.addAll(result)

    val outFile = projectRoot.resolve("output/debug.txt")
    outFile.parentFile.mkdirs()
    newBp.toJson().exportTo(outFile)
}
