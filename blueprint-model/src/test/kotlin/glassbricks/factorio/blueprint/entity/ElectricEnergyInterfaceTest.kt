package glassbricks.factorio.blueprint.entity

import kotlin.test.Test

class ElectricEnergyInterfaceTest {
    @Test
    fun `can load electric energy interfaces`() {
        testSaveLoad<ElectricEnergyInterface>("electric-energy-interface") {
            buffer_size = 1e9.toLong()
            power_production = 12345
            power_usage = 67890
        }
    }
}
