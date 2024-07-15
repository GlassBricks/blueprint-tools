package glassbricks.factorio.blueprint.prototypes

import glassbricks.factorio.dataRawUrl
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import kotlin.test.Test



@OptIn(ExperimentalSerializationApi::class)
class LoadKtTest {
    @Test
    fun `can load and save data raw`() {
        val dataRaw = DataRawJson.decodeFromStream(DataRaw.serializer(), dataRawUrl.openStream())
        println(dataRaw)
        DataRawJson.encodeToJsonElement(DataRaw.serializer(), dataRaw)
        val pipe = dataRaw.`pipe-to-ground`.values.first()
        val pipeStr = DataRawJson.encodeToJsonElement(pipe)
        println(pipeStr)
    }

    @Test
    fun `manual inspection`() {
        val orig = DataRawJson.decodeFromStream(JsonElement.serializer(), dataRawUrl.openStream())
        val origElectricInterface =
            orig.jsonObject["electric-energy-interface"]!!.jsonObject["electric-energy-interface"]!!

        val prettyJson = Json {
            prettyPrint = true
        }

        println(prettyJson.encodeToString(origElectricInterface))
    }
}
