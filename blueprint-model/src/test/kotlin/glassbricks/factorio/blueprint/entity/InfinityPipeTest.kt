package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityFilterMode
import glassbricks.factorio.blueprint.json.InfinitySettings
import org.junit.jupiter.api.Test

class InfinityPipeTest {
    @Test
    fun `can save and load`() {
        testSaveLoad<InfinityPipe>("infinity-pipe")
        testSaveLoad<InfinityPipe>("infinity-pipe") {
            infinity_settings = InfinitySettings(
                name = "water",
                mode = InfinityFilterMode.AtMost,
                percentage = 0.51,
                temperature = 15,
            )
        }
    }
}
