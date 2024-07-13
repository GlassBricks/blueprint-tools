package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test


val dataRawFile = object {}.javaClass.classLoader.getResourceAsStream("data-raw-dump.json")!!

@OptIn(ExperimentalSerializationApi::class)
class LoadKtTest {
    @Test
    fun `can load and save data raw`() {
        val dataRaw = DataRawJson.decodeFromStream(DataRaw.serializer(), dataRawFile)
        println(dataRaw)
        DataRawJson.encodeToJsonElement(DataRaw.serializer(), dataRaw)
        val pipe = dataRaw.`pipe-to-ground`.values.first()
        val pipeStr = DataRawJson.encodeToJsonElement(pipe)
        println(pipeStr)
    }
}
