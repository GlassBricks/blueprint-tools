import glassbricks.factorio.blueprint.entity.StorageTank
import glassbricks.factorio.blueprint.entity.testSaveLoad
import kotlin.test.Test

class StorageTankTest {
    @Test
    fun `can create storage tank`() {
        testSaveLoad(StorageTank::class, "storage-tank")
    }
}
