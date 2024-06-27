package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import org.junit.jupiter.api.Assertions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Icons are empty, which would be invalid in a real blueprint.
fun emptyBlueprint() = BlueprintJson(icons = emptyList())

class ImportExportKtTest {
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
                direction = glassbricks.factorio.blueprint.json.Direction.West
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

    @Test
    fun `can load electric pole connections`() {
        val blueprint = emptyBlueprint()
        blueprint.entities = listOf(
            buildEntityJson("small-electric-pole") {
                entity_number = EntityNumber(1)
                neighbours = listOf(EntityNumber(2), EntityNumber(3))
            },
            buildEntityJson("small-electric-pole") {
                entity_number = EntityNumber(2)
                neighbours = listOf(EntityNumber(1))
            },
            buildEntityJson("small-electric-pole") {
                entity_number = EntityNumber(3)
                neighbours = listOf(EntityNumber(1))
            }
        )
        val entities = blueprintPrototypes.entitiesFromJson(blueprint)
        assertNotNull(entities)
        val pole1 = entities[0] as ElectricPole
        val pole2 = entities[1] as ElectricPole
        val pole3 = entities[2] as ElectricPole
        Assertions.assertTrue(pole1.cableConnections.contains(pole2))
        Assertions.assertTrue(pole1.cableConnections.contains(pole3))
        Assertions.assertTrue(pole2.cableConnections.contains(pole1))
    }

    @Test
    fun `can save electric pole connections`() {
        val pole1 = loadEntity("small-electric-pole") as ElectricPole
        val pole2 = loadEntity("small-electric-pole") as ElectricPole
        val pole3 = loadEntity("small-electric-pole") as ElectricPole
        pole1.cableConnections.add(pole2)
        pole1.cableConnections.add(pole3)

        val blueprint = emptyBlueprint()
        blueprint.setEntitiesFrom(listOf(pole1, pole2, pole3))
        val pole1Json = blueprint.entities!![0]
        assertEquals(
            listOf(EntityNumber(2), EntityNumber(3)),
            pole1Json.neighbours
        )
        val pole2Json = blueprint.entities!![1]
        assertEquals(
            listOf(EntityNumber(1)),
            pole2Json.neighbours
        )
        val pole3Json = blueprint.entities!![2]
        assertEquals(
            listOf(EntityNumber(1)),
            pole3Json.neighbours
        )
    }

    @Test
    fun `can load circuit connections`() {
        val blueprint = emptyBlueprint()
        blueprint.entities = listOf(
            EntityJson(
                entity_number = EntityNumber(1),
                name = "foo1",
                position = Position.ZERO,
                connections = Connections(
                    `1` = ConnectionPoint(
                        red = listOf(ConnectionData(EntityNumber(2), CircuitID.First)),
                        green = listOf(ConnectionData(EntityNumber(3), CircuitID.First)),
                    ),
                    `2` = ConnectionPoint(
                        red = listOf(ConnectionData(EntityNumber(3), CircuitID.Second))
                    )
                )
            ),
            EntityJson(
                entity_number = EntityNumber(2),
                name = "foo2",
                position = Position.ZERO,
                connections = Connections(
                    `1` = ConnectionPoint(red = listOf(ConnectionData(EntityNumber(1), CircuitID.First)))
                )
            ),
            EntityJson(
                entity_number = EntityNumber(3),
                name = "foo3",
                position = Position.ZERO,
                connections = Connections(
                    `1` = ConnectionPoint(green = listOf(ConnectionData(EntityNumber(1), CircuitID.First))),
                    `2` = ConnectionPoint(red = listOf(ConnectionData(EntityNumber(1), CircuitID.Second))),
                )
            )
        )

        val entities = blueprintPrototypes.entitiesFromJson(blueprint)
        assertNotNull(entities)
        val (entity1, entity2, entity3) = entities
        assertTrue(entity1 is CircuitConnectable)
        assertTrue(entity2 is CircuitConnectable)
        assertTrue(entity3 is CircuitConnectable)

        assertEquals(setOf(entity2.connectionPoint1), entity1.connectionPoint1.red)
        assertEquals(setOf(entity3.connectionPoint1), entity1.connectionPoint1.green)
        assertEquals(setOf(entity3.connectionPoint2), entity1.connectionPoint2!!.red)

        assertEquals(setOf(entity1.connectionPoint1), entity2.connectionPoint1.red)

        assertEquals(setOf(entity1.connectionPoint1), entity3.connectionPoint1.green)
        assertEquals(setOf(entity1.connectionPoint2), entity3.connectionPoint2!!.red)
    }


    @Test
    fun `can save circuit connections`() {
        val entity1 = UnknownEntity("foo1", Position.ZERO)
        val entity2 = UnknownEntity("foo2", Position.ZERO)
        val entity3 = UnknownEntity("foo3", Position.ZERO)
        entity1.connectionPoint1.red.add(entity2.connectionPoint1)
        entity1.connectionPoint1.green.add(entity3.connectionPoint1)
        entity1.connectionPoint2.red.add(entity3.connectionPoint2)

        assertTrue(entity2.connectionPoint1.red.contains(entity1.connectionPoint1))

        val blueprint = emptyBlueprint()
        blueprint.setEntitiesFrom(listOf(entity1, entity2, entity3))
        val entity1Json = blueprint.entities!![0]
        assertEquals("foo1", entity1.name)
        assertEquals(
            Connections(
                ConnectionPoint(
                    red = listOf(ConnectionData(EntityNumber(2), CircuitID.First)),
                    green = listOf(ConnectionData(EntityNumber(3), CircuitID.First)),
                ),
                ConnectionPoint(
                    red = listOf(ConnectionData(EntityNumber(3), CircuitID.Second))
                )
            ),
            entity1Json.connections
        )
        val entity2Json = blueprint.entities!![1]
        assertEquals("foo2", entity2.name)
        assertEquals(
            Connections(
                `1` = ConnectionPoint(red = listOf(ConnectionData(EntityNumber(1), CircuitID.First)))
            ),
            entity2Json.connections
        )

        val entity3Json = blueprint.entities!![2]
        assertEquals("foo3", entity3.name)
        assertEquals(
            Connections(
                `1` = ConnectionPoint(green = listOf(ConnectionData(EntityNumber(1), CircuitID.First))),
                `2` = ConnectionPoint(red = listOf(ConnectionData(EntityNumber(1), CircuitID.Second))),
            ),
            entity3Json.connections
        )
    }
}
