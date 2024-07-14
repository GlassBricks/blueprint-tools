package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.test.Test


val dataRawFile = object {}.javaClass.classLoader.getResource("data-raw-dump.json")!!

@OptIn(ExperimentalSerializationApi::class)
class LoadKtTest {
    @Test
    fun `can load and save data raw`() {
        val dataRaw = DataRawJson.decodeFromStream(DataRaw.serializer(), dataRawFile.openStream())
        println(dataRaw)
        DataRawJson.encodeToJsonElement(DataRaw.serializer(), dataRaw)
        val pipe = dataRaw.`pipe-to-ground`.values.first()
        val pipeStr = DataRawJson.encodeToJsonElement(pipe)
        println(pipeStr)
    }

    @Test
    fun `manual inspection`() {
        val orig = DataRawJson.decodeFromStream(JsonElement.serializer(), dataRawFile.openStream())
        val origElectricInterface =
            orig.jsonObject["electric-energy-interface"]!!.jsonObject["electric-energy-interface"]!!

        val prettyJson = Json {
            prettyPrint = true
        }

        println(prettyJson.encodeToString(origElectricInterface))
    }
}
