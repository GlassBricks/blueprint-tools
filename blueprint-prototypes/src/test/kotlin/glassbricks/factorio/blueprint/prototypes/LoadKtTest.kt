package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test


@OptIn(ExperimentalSerializationApi::class)
class LoadKtTest {
    @Test
    fun `can load and save data raw`() {
        val dataRawFile = this.javaClass.classLoader.getResourceAsStream("data-raw-dump.json")!!
        val dataRaw = dataRawJson.decodeFromStream(DataRaw.serializer(), dataRawFile)
        println(dataRaw)
        dataRawJson.encodeToJsonElement(DataRaw.serializer(), dataRaw)
        val pipe = dataRaw.`pipe-to-ground`.values.first()
        val pipeStr = dataRawJson.encodeToJsonElement(pipe)
        println(pipeStr)
    }
}
