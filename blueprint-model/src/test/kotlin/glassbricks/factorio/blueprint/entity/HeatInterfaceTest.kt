package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityMode
import kotlin.test.Test

class HeatInterfaceTest {
    @Test
    fun `can save and load`() {
        testSaveLoad<HeatInterface>("heat-interface") {
            temperature = 100
            mode = InfinityMode.AtMost
        }
    }
}
