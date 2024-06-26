package glassbricks.factorio.blueprint

import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.test.Test


class ImportExportKtTest {

    private fun testLoadBlueprint(fileName: String) {
        val blueprintFile = File("test-blueprints/$fileName.txt")
        val bp = importBlueprintFromStream(blueprintFile.inputStream())
        println(bp)

        val roundTrip = bpJson.decodeFromString(ImportableBlueprint.serializer(), bpJson.encodeToString<ImportableBlueprint>(bp))
        assert(roundTrip == bp)
        
        val str = exportBlueprint(bp)
        println(str)
        val back = importBlueprint(str)
        assert(back == bp)
    }

    @Test
    fun `load bp1`() {
        testLoadBlueprint("bp1")
    }
    @Test
    fun `test base8`() {
        testLoadBlueprint("base8")
    }

}
