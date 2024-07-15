package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.json.Importable
import glassbricks.factorio.blueprint.json.bpJson
import glassbricks.factorio.blueprint.json.exportBlueprint
import glassbricks.factorio.blueprint.json.importBlueprint
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.test.Test


class ImportExportKtTest {

    private fun testLoadBlueprint(fileName: String) {
        val bp = importBlueprint(File("../test-blueprints/$fileName"))
        println(bp)

        val roundTrip = bpJson.decodeFromString(Importable.serializer(), bpJson.encodeToString<Importable>(bp))
        assert(roundTrip == bp)

        val str = exportBlueprint(bp)
        println(str)
        val back = importBlueprint(str)
        assert(back == bp)
    }

    @Test
    fun `load bp1`() {
        testLoadBlueprint("bp1.txt")
    }

    @Test
    fun `test base8`() {
        testLoadBlueprint("base8.txt")
    }

    @Test
    fun `test blueprint book`() {
        testLoadBlueprint("book.txt")
    }

}
