package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlin.test.Test


@OptIn(ExperimentalSerializationApi::class)
class LoadKtTest {
    @Test
    fun `can load and save data raw`() {
        val dataRawFile = this.javaClass.classLoader.getResourceAsStream("data-raw-dump.json")!!
        val dataRaw = dataRawJson.decodeFromStream(DataRaw.serializer(), dataRawFile)
        println(dataRaw)
        val saved = dataRawJson.encodeToString(DataRaw.serializer(), dataRaw)
        println(saved)
    }
}
