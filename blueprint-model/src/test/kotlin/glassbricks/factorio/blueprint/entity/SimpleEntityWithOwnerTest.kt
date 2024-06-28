package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class SimpleEntityWithOwnerTest {
    @Test
    fun `can save load`() {
        testSaveLoad<SimpleEntityWithOwner>("simple-entity-with-owner") {
            variation = 2.toUByte()
        }
        testSaveLoad<SimpleEntityWithOwner>("simple-entity-with-force") {
            variation = 2.toUByte()
        }
    }
}
