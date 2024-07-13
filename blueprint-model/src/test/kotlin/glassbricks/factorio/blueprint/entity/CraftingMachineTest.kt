package glassbricks.factorio.blueprint.entity

import org.junit.jupiter.api.Test

class CraftingMachineTest {
    @Test
    fun `can load assembling machines`() {
        testSaveLoad(AssemblingMachine::class, "assembling-machine-1", null, false) {
            recipe = "iron-plate"
            items = mapOf("productivity-module" to 1)
        }
        testSaveLoad(Furnace::class, "electric-furnace", null, false) {
            items = mapOf("productivity-module" to 1)
        }
        testSaveLoad(AssemblingMachine::class, "chemical-plant", null, false) {
            recipe = "sulfur"
            items = mapOf("productivity-module" to 1)
        }
        testSaveLoad(RocketSilo::class, "rocket-silo", null, false) {
            recipe = "rocket-part"
            auto_launch = true
        }
    }
}
