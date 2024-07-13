package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityMode
import kotlin.test.Test

class HeatInterfaceTest {
    @Test
    fun `can save and load`() {
        testSaveLoad(HeatInterface::class, "heat-interface", null, false, fun EntityJson.() {
            temperature = 100
            mode = InfinityMode.AtMost
        })
    }
}
