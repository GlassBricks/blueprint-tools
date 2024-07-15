package glassbricks.factorio.generatedataraw

import glassbricks.factorio.blueprint.prototypes.DataRaw
import glassbricks.factorio.blueprint.prototypes.DataRawJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File


@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val outputFile = args[0]
    val dataRawDump = object {}.javaClass.getResource("/data-raw-dump.json")!!
    val dataRaw = DataRawJson.decodeFromStream(DataRaw.serializer(), dataRawDump.openStream())
    File(outputFile).outputStream().use {
        DataRawJson.encodeToStream(DataRaw.serializer(), dataRaw, it)
    }
}
