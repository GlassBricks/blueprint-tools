package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class StorageTankTest {
    @Test
    fun `can create storage tank`() {
        testSaveLoad(StorageTank::class, "storage-tank")
    }
}
