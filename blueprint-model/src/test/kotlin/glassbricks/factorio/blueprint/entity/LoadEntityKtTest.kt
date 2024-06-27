package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.EntityNumber
import glassbricks.factorio.blueprint.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Icons are empty, which would be invalid in a real blueprint.
fun emptyBlueprint() = BlueprintJson(icons = emptyList())

class LoadEntityKtTest {
    @Test
    fun `can create simple entity`() {
        val entityJson = EntityJson(
            entity_number = EntityNumber(1),
            name = "iron-chest",
            position = Position(1.5, 2.5),
            bar = 1,
        )
        val entity = blueprintPrototypes.createEntityFromJson(entityJson)
        assertEquals(entity.name, "iron-chest")
        assertEquals(entity.type, "container")
        assertEquals(entity.prototype, blueprintPrototypes.prototypes["iron-chest"])

        val backToJson = entity.toJsonIsolated(EntityNumber(2))
        assertEquals(backToJson.entity_number, EntityNumber(2))

        backToJson.entity_number = EntityNumber(1)
        assertEquals(entityJson, backToJson)
    }
    @Test
    fun `can load an unknown entity`() {
        val entityJson = EntityJson(
            entity_number = EntityNumber(1),
            name = "foo",
            position = Position(1.0, 2.0),
        )
        val entity = blueprintPrototypes.createEntityFromJson(entityJson)
        assertTrue(entity is UnknownEntity)
        assertEquals(entity.name, "foo")
        assertEquals(entity.type, "unknown")
        assertTrue(entity.prototype is UnknownPrototype)

        val backToJson = entity.toJsonIsolated(EntityNumber(2))
        assertEquals(backToJson.entity_number, EntityNumber(2))

        backToJson.entity_number = EntityNumber(1)
        assertEquals(entityJson, backToJson)
    }

    @Test
    fun `can set entities`() {
        val blueprint = emptyBlueprint()
        val entities = listOf(
            loadEntity("foo1") {
                position = Position(1.0, 2.0)
            },
            loadEntity("foo2") {
                position = Position(3.0, 4.0)
                direction = Direction.West
            },
        )
        entities.forEach {
            assertTrue(it is UnknownEntity)
        }
        blueprint.setEntitiesFrom(entities)
        assertEquals(
            blueprint.entities, listOf(
                EntityJson(
                    entity_number = EntityNumber(1),
                    name = "foo1",
                    position = Position(1.0, 2.0),
                ),
                EntityJson(
                    entity_number = EntityNumber(2),
                    name = "foo2",
                    position = Position(3.0, 4.0),
                    direction = Direction.West,
                ),
            )
        )
    }
}
