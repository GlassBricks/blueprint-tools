package glassbricks.factorio.blueprint.entity

import org.junit.jupiter.api.Test

class CraftingMachineTest {
    @Test
    fun `can load assembling machines`() {
        testSaveLoad(AssemblingMachine::class, "assembling-machine-1", null, false, fun EntityJson.() {
            recipe = "iron-plate"
            items = mapOf("productivity-module" to 1)
        })
        testSaveLoad(Furnace::class, "electric-furnace", null, false, fun EntityJson.() {
            items = mapOf("productivity-module" to 1)
        })
        testSaveLoad(AssemblingMachine::class, "chemical-plant", null, false, fun EntityJson.() {
            recipe = "sulfur"
            items = mapOf("productivity-module" to 1)
        })
        testSaveLoad(RocketSilo::class, "rocket-silo", null, false, fun EntityJson.() {
            recipe = "rocket-part"
            auto_launch = true
        })
    }
}
