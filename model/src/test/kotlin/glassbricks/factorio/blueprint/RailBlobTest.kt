package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.entity.StraightRail
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RailBlobTest {
    @Test
    fun `test rail blob`() {
        val bp = Blueprint(importBlueprintFrom(File("../test-blueprints/rail-blob.txt")))
        bp.entities.forEach { entity ->
            val colliding = bp.entities.getColliding(entity).toList()
                .filter { it != entity }
            assertTrue(colliding.none(), "Entity $entity collides with $colliding")
        }
    }

    @Test
    fun `diagonal rail collision box`() {
        val rail = VanillaPrototypes.createBpEntity<StraightRail>("straight-rail", Position.ZERO, Direction.Northeast)
        val topLeft = tilePos(-1, -2)
        fun assertCollides(x: Int, y: Int, expected: Boolean) {
            val actual = rail.intersects(tilePos(x, y).tileBoundingBox())
            assertEquals(expected, actual, "($x, $y)")
        }
        for (i in 0..2) for (j in 0..2) {
            val isCorner = (i != 1) && (j != 1)
            assertCollides(topLeft.x + i, topLeft.y + j, !isCorner)
        }
    }

}
