package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class SimpleEntityWithOwnerTest {
    @Test
    fun `can save load`() {
        testSaveLoad(SimpleEntityWithOwner::class, "simple-entity-with-owner", null, false) {
            variation = 2.toUByte()
        }
        testSaveLoad(SimpleEntityWithOwner::class, "simple-entity-with-force", null, false) {
            variation = 2.toUByte()
        }
    }
}
