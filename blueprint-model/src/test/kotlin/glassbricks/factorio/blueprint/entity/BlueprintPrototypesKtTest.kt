package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.json.Position
import java.io.File
import kotlin.test.*


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

internal inline fun loadEntity(
    name: String,
    blueprint: BlueprintJson? = null,
    build: EntityJson.() -> Unit = {},
) = blueprintPrototypes.createEntityFromJson(buildEntityJson(name, build), blueprint)

internal inline fun <reified T : Entity> testSaveLoad(
    json: EntityJson,
    blueprint: BlueprintJson?,
): T {
    json.entity_number = EntityNumber(1)
    val entity = blueprintPrototypes.createEntityFromJson(json, blueprint)
    assertTrue(entity is T, "Expected ${T::class.java} but got ${entity.javaClass}")
    assertEquals(entity.javaClass, T::class.java, "Expected exactly, ${T::class.java} but got subclass ${entity.javaClass}")

    val backToJson = entity.toJsonIsolated(EntityNumber(1))
    assertEquals(json, backToJson)
    return entity
}
internal inline fun <reified T : Entity> testSaveLoad(
    name: String,
    blueprint: BlueprintJson? = null,
    build: EntityJson.() -> Unit = {},
): T {
    val json = buildEntityJson(name, build)
    return testSaveLoad(json, blueprint)
}

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
