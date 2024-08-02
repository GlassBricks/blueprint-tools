package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class LabTest {
    @Test
    fun `can load lab`() {
        testSaveLoad(Lab::class, "lab")
        testSaveLoad(Lab::class, "lab") {
            items = mapOf("item" to 1)
        }
    }
}
