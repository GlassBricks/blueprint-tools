package glassbricks.factorio.blueprint.prototypes

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.pos
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityUtilsKtTest {

    @Test
    fun energySource() {
        val prototype = VanillaPrototypes.get<InserterPrototype>("inserter")
        assertTrue(prototype.energySource is ElectricEnergySource)
        assertTrue(prototype.usesElectricity)

        val chest = VanillaPrototypes.get<ContainerPrototype>("wooden-chest")
        assertTrue(chest.energySource == null)
        assertFalse(chest.usesElectricity)

        val furnace = VanillaPrototypes.get<FurnacePrototype>("stone-furnace")
        assertTrue(furnace.energySource is BurnerEnergySource)
        assertFalse(furnace.usesElectricity)
    }

    val entity1x1 = VanillaPrototypes.get<InserterPrototype>("inserter")
    val entity2x2 = VanillaPrototypes.get<EntityWithOwnerPrototype>("stone-furnace")
    val entity3x2 = VanillaPrototypes.get<BoilerPrototype>("boiler")

    @Test
    fun `effective tile width and height`() {

        assertEquals(1, entity1x1.effectiveTileWidth)
        assertEquals(1, entity1x1.effectiveTileHeight)

        assertEquals(2, entity2x2.effectiveTileWidth)
        assertEquals(2, entity2x2.effectiveTileHeight)

        assertEquals(3, entity3x2.effectiveTileWidth)
        assertEquals(2, entity3x2.effectiveTileHeight)
    }

    @Test
    fun tileSnappedPosition() {
        assertEquals(pos(1.5, 2.5), entity1x1.tileSnappedPosition(tilePos(1, 2)))

        assertEquals(pos(2.0, 3.0), entity2x2.tileSnappedPosition(tilePos(1, 2)))

        assertEquals(pos(2.5, 3.0), entity3x2.tileSnappedPosition(tilePos(1, 2)))
        assertEquals(pos(2.5, 3.0), entity3x2.tileSnappedPosition(tilePos(1, 2), Direction.South))
        assertEquals(pos(2.0, 3.5), entity3x2.tileSnappedPosition(tilePos(1, 2), Direction.East))
        assertEquals(pos(2.0, 3.5), entity3x2.tileSnappedPosition(tilePos(1, 2), Direction.West))
    }

    @Test
    fun topLeftTileAt() {
        assertEquals(tilePos(1, 2), entity1x1.topLeftTileAt(pos(1.5, 2.5)))

        assertEquals(tilePos(1, 2), entity2x2.topLeftTileAt(pos(2.0, 3.0)))

        assertEquals(tilePos(1, 2), entity3x2.topLeftTileAt(pos(2.5, 3.0)))
        assertEquals(tilePos(1, 2), entity3x2.topLeftTileAt(pos(2.5, 3.0), Direction.South))
        assertEquals(tilePos(1, 2), entity3x2.topLeftTileAt(pos(2.0, 3.5), Direction.East))
        assertEquals(tilePos(1, 2), entity3x2.topLeftTileAt(pos(2.0, 3.5), Direction.West))
    }
}
