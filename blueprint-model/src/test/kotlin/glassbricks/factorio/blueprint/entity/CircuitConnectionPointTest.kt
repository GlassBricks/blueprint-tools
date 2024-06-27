package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Position
import glassbricks.factorio.blueprint.json.*
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*
import glassbricks.factorio.blueprint.json.ConnectionPoint as ConnectionPointJson

class CircuitConnectionPointTest {
    lateinit var point1: CircuitConnectionPoint
    lateinit var point2: CircuitConnectionPoint
    lateinit var point3: CircuitConnectionPoint

    @BeforeEach
    fun setUp() {
        point1 = CircuitConnectionPoint(CircuitID.First)
        point2 = CircuitConnectionPoint(CircuitID.Second)
        point3 = CircuitConnectionPoint(CircuitID.First)
    }

    @Test
    fun `isEmpty returns if connections are empty`() {
        assertTrue(point1.isEmpty())
        assertTrue(point2.isEmpty())
        point1.red.add(point2)
        assertFalse(point1.isEmpty())
        assertFalse(point2.isEmpty())
    }

    @Test
    fun `adding a connection also adds the reverse connection`() {
        point1.red.add(point2)
        assertTrue(point2.red.contains(point1))
        assertFalse(point2.green.contains(point1))

        point2.green.add(point1)
        assertTrue(point1.green.contains(point2))
        assertTrue(point1.red.contains(point2))
    }

    @Test
    fun `removing a connection also removes the reverse connection`() {
        point1.red.add(point2)
        point2.red.remove(point1)

        assertFalse(point2.green.contains(point1))
        assertFalse(point1.red.contains(point2))
    }

    @Test
    fun `clear removes all connections`() {
        point1.red.add(point2)
        point1.green.add(point3)

        point1.clear()
        assertFalse(point1.red.contains(point2))
        assertFalse(point1.green.contains(point3))
        assertFalse(point3.green.contains(point1))
        assertFalse(point2.red.contains(point1))
    }

    @Test
    fun `export exports correctly`() {
        val parentMap = mapOf(
            point1 to EntityNumber(1), point2 to EntityNumber(2), point3 to EntityNumber(3)
        )
        assertNull(point1.export(parentMap))
        assertNull(point2.export(parentMap))

        point1.red.add(point2)
        point1.green.add(point3)

        val data = point1.export(parentMap)
        assertNotNull(data)
        assertEquals(
            data.red, listOf(ConnectionData(EntityNumber(2), CircuitID.Second))
        )
        assertEquals(
            data.green, listOf(ConnectionData(EntityNumber(3), CircuitID.First))
        )

        val data2 = point2.export(parentMap)
        assertNotNull(data2)
        assertEquals(
            data2.red, listOf(ConnectionData(EntityNumber(1), CircuitID.First))
        )
        assertNull(data2.green)

        val data3 = point3.export(parentMap)
        assertNotNull(data3)
        assertNull(data3.red)
        assertEquals(
            data3.green, listOf(ConnectionData(EntityNumber(1), CircuitID.First))
        )
    }
}

class CircuitConnectableKtTest {

    @Test
    fun `can load circuit connections`() {
        val blueprint = emptyBlueprint()
        blueprint.entities = listOf(
            EntityJson(
                entity_number = EntityNumber(1),
                name = "foo1",
                position = Position.ZERO,
                connections = Connections(
                    `1` = ConnectionPointJson(
                        red = listOf(ConnectionData(EntityNumber(2), CircuitID.First)),
                        green = listOf(ConnectionData(EntityNumber(3), CircuitID.First)),
                    ),
                    `2` = ConnectionPointJson(
                        red = listOf(ConnectionData(EntityNumber(3), CircuitID.Second))
                    )
                )
            ),
            EntityJson(
                entity_number = EntityNumber(2),
                name = "foo2",
                position = Position.ZERO,
                connections = Connections(
                    `1` = ConnectionPointJson(red = listOf(ConnectionData(EntityNumber(1), CircuitID.First)))
                )
            ),
            EntityJson(
                entity_number = EntityNumber(3),
                name = "foo3",
                position = Position.ZERO,
                connections = Connections(
                    `1` = ConnectionPointJson(green = listOf(ConnectionData(EntityNumber(1), CircuitID.First))),
                    `2` = ConnectionPointJson(red = listOf(ConnectionData(EntityNumber(1), CircuitID.Second))),
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
                ConnectionPointJson(
                    red = listOf(ConnectionData(EntityNumber(2), CircuitID.First)),
                    green = listOf(ConnectionData(EntityNumber(3), CircuitID.First)),
                ),
                ConnectionPointJson(
                    red = listOf(ConnectionData(EntityNumber(3), CircuitID.Second))
                )
            ),
            entity1Json.connections
        )
        val entity2Json = blueprint.entities!![1]
        assertEquals("foo2", entity2.name)
        assertEquals(
            Connections(
                `1` = ConnectionPointJson(red = listOf(ConnectionData(EntityNumber(1), CircuitID.First)))
            ),
            entity2Json.connections
        )

        val entity3Json = blueprint.entities!![2]
        assertEquals("foo3", entity3.name)
        assertEquals(
            Connections(
                `1` = ConnectionPointJson(green = listOf(ConnectionData(EntityNumber(1), CircuitID.First))),
                `2` = ConnectionPointJson(red = listOf(ConnectionData(EntityNumber(1), CircuitID.Second))),
            ),
            entity3Json.connections
        )
    }
}
