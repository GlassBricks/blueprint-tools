package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue


val blueprintPrototypes by lazy {
    val file = File("../blueprint-prototypes/src/test/resources/data-raw-dump.json")
    BlueprintPrototypes.loadFromDataRaw(file)
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
    connectToNetwork: Boolean,
    blueprint: BlueprintJson?,
): T {
    json.entity_number = EntityNumber(1)
    val entity = blueprintPrototypes.createEntityFromJson(json, blueprint)
    assertTrue(entity is T, "Expected ${T::class.java} but got ${entity.javaClass}")
    assertEquals(
        entity.javaClass,
        T::class.java,
        "Expected exactly, ${T::class.java} but got subclass ${entity.javaClass}"
    )

    if (connectToNetwork) {
        val other = UnknownEntity("foo", Position.ZERO)
        (entity as CircuitConnectionPoint).circuitConnections.red.add(other)
    }

    val backToJson = entity.toJsonIsolated(EntityNumber(1))
    assertEquals(json, backToJson)
    return entity
}

internal inline fun <reified T : Entity> testSaveLoad(
    name: String,
    blueprint: BlueprintJson? = null,
    connectToNetwork: Boolean = false,
    build: EntityJson.() -> Unit = {},
): T {
    val json = buildEntityJson(name, build)
    return testSaveLoad(json, connectToNetwork, blueprint)
}
