package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.entity.TransportBelt
import glassbricks.factorio.blueprint.entity.placedAtTile
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.ops.getBeltLines
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetBeltLineTest {

    @Test
    fun `getting straight belt line`() {
        val entities = createEntities("> # <=>    <", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.tiles.size }
        assertEquals(12, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)
        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertTrue(line.tiles.all { it.allowedBeltTiers.single() == normalBelt })
        assertTrue(line.tiles.all { it.mustMatch == null })
    }

    @Test
    fun `getting belt line with side loading`() {
        val entities = createEntities("===+", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.tiles.size }
        assertEquals(4, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)
        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertEquals(1, line.tiles.count { it.mustMatch != null })
        assertEquals(line.tiles[3].mustMatch, normalBelt.belt)
    }

    @Test
    fun `getting ug belt with sideloading`() {
        val entities = createEntities("=/==\\=", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.tiles.size }
        assertEquals(6, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)

        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertEquals(2, line.tiles.count { it.mustMatch != null })
        assertEquals(normalBelt.inputUg, line.tiles[1].mustMatch)
        assertEquals(normalBelt.outputUg, line.tiles[4].mustMatch)
    }

    @Test
    fun `getting belt line with circuit connections`() {
        val entities = createEntities("=====", startPos = tilePos(0, 0))
        val entity4 = entities.getInTile(tilePos(3, 0)).first() as TransportBelt
        val entity5 = entities.getInTile(tilePos(4, 0)).first() as TransportBelt
        entity4.circuitConnections.red.add(entity5)
        val lines = getBeltLines(entities)
        val line = lines.maxBy { it.tiles.size }
        assertEquals(5, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)
        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertEquals(2, line.tiles.count { it.mustMatch != null })
        assertEquals(normalBelt.belt, line.tiles[3].mustMatch)
        assertEquals(normalBelt.belt, line.tiles[4].mustMatch)
    }

    @Test
    fun `getting belt with inserter connections`() {
        val entities = createEntities("==v=^=", startPos = tilePos(0, 0))
        val lines = getBeltLines(entities)
        assertEquals(1, lines.size)
        val line = lines.first()
        assertEquals(6, line.tiles.size)
        assertEquals(2, line.tiles.count { it.mustBeNotEmpty })
        assertTrue(line.tiles[2].mustBeNotEmpty)
        assertTrue(line.tiles[4].mustBeNotEmpty)
        assertTrue(line.tiles.none { it.mustMatch != null })
    }


    @Test
    fun `isolated output underground belt`() {
        val entities = createEntities("<", startPos = tilePos(0, 0))
        val line = getBeltLines(entities).find { it.direction == CardinalDirection.East }!!
        assertEquals(1, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)
        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertEquals(1, line.tiles.count { it.mustMatch != null })
        assertEquals(normalBelt.outputUg.copy(isIsolated = true), line.tiles[0].mustMatch)
    }

    @Test
    fun `isolated input underground belt`() {
        val entities = createEntities(">", startPos = tilePos(0, 0))
        val line = getBeltLines(entities).find { it.direction == CardinalDirection.East }!!
        assertEquals(1, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)
        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertEquals(1, line.tiles.count { it.mustMatch != null })
        assertEquals(normalBelt.inputUg.copy(isIsolated = true), line.tiles[0].mustMatch)
    }

    @Test
    fun `momentarily upgraded belt`() {
        val entities = createEntities("=v^==", startPos = tilePos(0, 0))
        entities.removeAll(entities.getInTile(tilePos(1, 0)))
        entities.removeAll(entities.getInTile(tilePos(2, 0)))
        entities.add(fastTier.beltProto.placedAtTile(tilePos(1, 0), Direction.East))
        entities.add(fastTier.beltProto.placedAtTile(tilePos(2, 0), Direction.East))

        val lines = getBeltLines(entities)
        val line = lines.single()

        assertEquals(5, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)

        assertEquals(line.tiles.withIndex().filter { it.value.mustBeNotEmpty }.map { it.index }, listOf(1, 2))
        assertEquals(listOf(fastTier), line.tiles[1].allowedBeltTiers)
        assertEquals(listOf(fastTier), line.tiles[2].allowedBeltTiers)
        assertTrue(listOf(0, 3, 4).all {
            line.tiles[it].allowedBeltTiers == listOf(normalBelt)
        })
    }

    @Test
    fun `single tile upgrade belt`() {
        val entities = createEntities("=|==", startPos = tilePos(0, 0))
        entities.removeAll(entities.getInTile(tilePos(1, 0)))
        entities.add(fastTier.beltProto.placedAtTile(tilePos(1, 0), Direction.East))

        val line = getBeltLines(entities).find { it.direction == CardinalDirection.East }!!
        assertEquals(4, line.tiles.size)
        assertEquals(listOf(fastTier), line.tiles[1].allowedBeltTiers)
        assertTrue(listOf(0, 2, 3).all {
            line.tiles[it].allowedBeltTiers == listOf(normalBelt)
        })
    }

    @Test
    fun `multiple belt types`() {
        val entities = createEntities("=====", startPos = tilePos(0, 0))
        entities.removeAll(entities.getInTile(tilePos(1, 0)))
        entities.add(fastTier.beltProto.placedAtTile(tilePos(1, 0), Direction.East))

        val line = getBeltLines(entities).single()
        val allTiers = listOf(normalBelt, fastTier)

        assertEquals(5, line.tiles.size)
        assertEquals(CardinalDirection.East, line.direction)

        assertTrue(line.tiles.none { it.mustBeNotEmpty })
        assertTrue(line.tiles.all { it.mustMatch == null })
        assertTrue(line.tiles.all { it.allowedBeltTiers == allTiers })
    }
}
