package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityMode
import glassbricks.factorio.blueprint.json.InfinitySettings
import org.junit.jupiter.api.Test

class InfinityPipeTest {
    @Test
    fun `can save and load`() {
        testSaveLoad(InfinityPipe::class, "infinity-pipe")
        testSaveLoad(InfinityPipe::class, "infinity-pipe", null, false, fun EntityJson.() {
            infinity_settings = InfinitySettings(
                name = "water",
                mode = InfinityMode.AtMost,
                percentage = 0.51,
                temperature = 15,
            )
        })
    }
}
