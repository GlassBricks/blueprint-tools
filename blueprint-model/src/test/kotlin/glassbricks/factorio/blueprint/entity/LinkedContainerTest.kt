package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class LinkedContainerTest {
    @Test
    fun `can load linked containers`() {
        testSaveLoad(LinkedContainer::class, "linked-chest", null, false, fun EntityJson.() {
            bar = 1
            link_id = 2
        })
    }
}
