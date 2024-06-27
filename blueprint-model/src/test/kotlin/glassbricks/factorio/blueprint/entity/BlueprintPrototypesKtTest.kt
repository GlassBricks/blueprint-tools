package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.EntityNumber
import glassbricks.factorio.blueprint.Position
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull


val blueprintPrototypes by lazy {
    val file = File("../prototypes/src/test/resources/data-raw-dump.json")
    BlueprintPrototypes.fromDataRawFile(file)
}

inline fun buildEntityJson(
    name: String,
    build: EntityJson.() -> Unit = {},
) = EntityJson(
    entity_number = EntityNumber(1),
    name = name,
    position = Position.ZERO,
).apply(build)

inline fun loadEntity(
    name: String,
    blueprint: BlueprintJson? = null,
    build: EntityJson.() -> Unit = {},
) = blueprintPrototypes.createEntityFromJson(buildEntityJson(name, build), blueprint)

class BlueprintPrototypesKtTest {
    @Test
    fun `can load from data raw`() {

        assertEquals(blueprintPrototypes.prototypes.keys, blueprintPrototypes.placeableBy.keys)

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
            val entity = blueprintPrototypes.prototypes[name]
            assertNotNull(entity, "Entity $name not found in prototypeMap")
            assertEquals(entity.name, name)
        }
        for (name in arrayOf(
            "spidertron",
            "car"
        )) {
            assertFalse(name in blueprintPrototypes.prototypes)
        }
    }

}
