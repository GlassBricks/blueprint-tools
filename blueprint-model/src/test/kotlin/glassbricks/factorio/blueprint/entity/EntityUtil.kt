package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import java.io.File
import kotlin.reflect.KClass
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

internal inline fun <reified T> loadEntity(
    name: String,
    blueprint: BlueprintJson? = null,
    build: EntityJson.() -> Unit = {},
): T = blueprintPrototypes.entityFromJson(buildEntityJson(name, build), blueprint) as T

internal fun <T : Entity> testSaveLoad(
    klass: KClass<T>,
    json: EntityJson,
    connectToNetwork: Boolean,
    blueprint: BlueprintJson?,
): T {
    json.entity_number = EntityNumber(1)
    val entity = blueprintPrototypes.entityFromJson(json, blueprint)
    assertTrue(klass.isInstance(entity), "Expected $klass but got ${entity.javaClass}")

    assertEquals(
        entity::class,
        klass,
        "Expected exactly, $klass but got subclass ${entity::class}"
    )

    if (connectToNetwork) {
        val other = UnknownEntity("foo", Position.ZERO)
        (entity as CircuitConnectionPoint).circuitConnections.red.add(other)
    }

    val backToJson = entity.toJsonIsolated(EntityNumber(1))
    assertEquals(json, backToJson)
    @Suppress("UNCHECKED_CAST")
    return entity as T
}

internal fun <T : Entity> testSaveLoad(
    klass: KClass<T>,
    name: String,
    blueprint: BlueprintJson? = null,
    connectToNetwork: Boolean = false,
    build: EntityJson.() -> Unit = {},
): T {
    val json = buildEntityJson(name, build)
    return testSaveLoad(klass, json, connectToNetwork, blueprint)
}
