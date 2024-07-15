package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.entity.blueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.FurnacePrototype
import kotlin.test.Test
import kotlin.test.assertEquals

class SpatialTest {
    @Test
    fun `entity spatial`() {
        val prototype = blueprintPrototypes.getPrototype<FurnacePrototype>("stone-furnace")
        val spatial = EntitySpatial(prototype, pos(1.0, 2.0))
        assertEquals(prototype, spatial.prototype)
        assertEquals(pos(1.0, 2.0), spatial.position)
        assertEquals(tileBbox(0, 1, 2, 3), spatial.collisionBox.roundOutToTileBbox())
    }
}
