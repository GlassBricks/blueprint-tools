package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

/** pole connection tests in [ImportExportKtTest] */
class ElectricPoleTest {
    @Test
    fun `can create ElectricPole`() {
        testSaveLoad(ElectricPole::class, "small-electric-pole")
    }
}
