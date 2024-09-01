package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.json.*
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.test.Test


class ImportExportKtTest {

    private fun testLoadBlueprint(fileName: String) {
        val bp = importBlueprintJson(File("../test-blueprints/$fileName"))
        println(bp)

        val roundTrip = bpJson.decodeFromString(Importable.serializer(), bpJson.encodeToString<Importable>(bp))
        assert(roundTrip == bp)

        val str = bp.exportToString()
        println(str)
        val back = importBlueprintString(str)
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
