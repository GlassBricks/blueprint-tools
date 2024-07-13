package glassbricks.factorio.blueprint.entity

import org.junit.jupiter.api.Test

class CraftingMachineTest {
    @Test
    fun `can load assembling machines`() {
        testSaveLoad<AssemblingMachine>("assembling-machine-1") {
            recipe = "iron-plate"
            items = mapOf("productivity-module" to 1)
        }
        testSaveLoad<Furnace>("electric-furnace") {
            items = mapOf("productivity-module" to 1)
        }
        testSaveLoad<AssemblingMachine>("chemical-plant") {
            recipe = "sulfur"
            items = mapOf("productivity-module" to 1)
        }
        testSaveLoad<RocketSilo>("rocket-silo") {
            recipe = "rocket-part"
            auto_launch = true
        }
    }
}
