package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class BeaconTest {
    @Test
    fun `can load beacon`() {
        testSaveLoad(Beacon::class, "beacon")
        testSaveLoad(Beacon::class, "beacon", null, false, fun EntityJson.() {
            items = mapOf("thing" to 1)
        })
    }
}
