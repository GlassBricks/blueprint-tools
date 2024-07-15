package glassbricks.factorio

import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import java.io.File

val dataRawUrl = object {}.javaClass.getResource("/data-raw-dump.json")!!

val blueprintPrototypes by lazy { BlueprintPrototypes.loadFromDataRaw(dataRawUrl) }

fun blueprint(fileName: String): File = File("../test-blueprints/$fileName")
