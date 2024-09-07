package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.json.exportToString
import java.io.File
import kotlin.test.Test


class ImportExportKtTest {

    private fun testLoadBlueprint(fileName: String) {
        val bp = importBlueprintFrom(File("../test-blueprints/$fileName"))
        println(bp)

        val roundTrip = importBlueprintString(bp.exportToString())
        assert(roundTrip == bp)
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
