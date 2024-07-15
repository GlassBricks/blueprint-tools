package glassbricks.factorio.blueprint.prototypes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class BlueprintJsonPrototypesTest {

    @Test
    fun `can load blueprint prototypes`() {
        println(VanillaPrototypes.blueprintableEntities)
    }

    @Test
    fun `can load from data raw`() {
        assertEquals(VanillaPrototypes.blueprintableEntities.keys, VanillaPrototypes.itemsToPlace.keys)

        for (name in arrayOf(
            "wooden-chest",
            "linked-chest",
            "curved-rail",
            "big-electric-pole",
            "rocket-silo",
            "fast-loader",
            "cargo-wagon",
            "artillery-wagon",
            "locomotive",
        )) {
            val entity = VanillaPrototypes.blueprintableEntities[name]
            assertNotNull(entity, "Entity $name not found in prototypeMap")
            assertEquals(entity.name, name)
        }
        for (name in arrayOf(
            "spidertron",
            "car"
        )) {
            assertFalse(name in VanillaPrototypes.blueprintableEntities)
        }
    }

}
