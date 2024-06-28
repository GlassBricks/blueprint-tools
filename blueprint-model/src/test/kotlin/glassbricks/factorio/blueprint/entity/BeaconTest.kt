package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class BeaconTest {
    @Test
    fun `can load beacon` () {
        testSaveLoad<Beacon>("beacon")
        testSaveLoad<Beacon>("beacon") {
            items= mapOf("thing" to 1)
        }
    }
}
