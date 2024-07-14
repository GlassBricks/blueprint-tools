package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.pos
import glassbricks.factorio.blueprint.prototypes.CollisionMask
import glassbricks.factorio.blueprint.prototypes.SimpleEntityWithOwnerPrototype
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// This is also a test for BaseEntity
class BasicEntityJsonTest {


    @Test
    fun `bounding box and intersection`() {
        val testPrototype = object : SimpleEntityWithOwnerPrototype() {
            init {
                type = "test"
                name = "foo"
                collision_box = BoundingBox(-0.3, -0.5, 0.3, 0.5)
                collision_mask = CollisionMask.DEFAULT_MASKS["accumulator"]!!
            }
        }
        val entity = BasicEntity(
            testPrototype,
            EntityJson(EntityNumber(1), "foo", pos(2.0, 3.0), Direction.East)
        )

        assertEquals("foo", entity.name)
        assertEquals("test", entity.type)
        assertEquals(pos(2.0, 3.0), entity.position)
        assertEquals(Direction.East, entity.direction)
        assertEquals(testPrototype, entity.prototype)

        assertEquals(
            BoundingBox(-0.5, -0.3, 0.5, 0.3).translate(2.0, 3.0),
            entity.collisionBox
        )
        assertEquals(CollisionMask.DEFAULT_MASKS["accumulator"]!!, entity.collisionMask)

        val json = entity.toJsonIsolated(EntityNumber(2))
        val expected =
            EntityJson(EntityNumber(2), "foo", pos(2.0, 3.0), Direction.East)
        assertEquals(expected, json)


        val entity2 = BasicEntity(
            testPrototype,
            EntityJson(EntityNumber(1), "foo", pos(2.0, 3.2), Direction.East)
        )
        assertTrue(entity.collidesWith(entity2))
        assertTrue(entity2.collidesWith(entity))
        val entity3 = BasicEntity(
            testPrototype,
            EntityJson(EntityNumber(1), "foo", pos(2.0, 4.0), Direction.East)
        )
        assertFalse(entity.collidesWith(entity3))
        assertFalse(entity3.collidesWith(entity))
    }
}
