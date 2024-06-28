package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class LinkedContainerTest {
    @Test
    fun `can load linked containers`() {
        testSaveLoad<LinkedContainer>("linked-chest") {
            bar = 1
            link_id = 2
        }
    }
}
