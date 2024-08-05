package glassbricks.factorio.blueprint.placement

import glassbricks.factorio.blueprint.pos
import glassbricks.factorio.blueprint.prototypes.tileSnappedPosition
import glassbricks.factorio.blueprint.tileBbox
import glassbricks.factorio.blueprint.tilePos
import kotlin.test.*


class EntityPlacementModelTest {
    val model = EntityPlacementModel()

    @Test
    fun addFixedEntity() {
        val placement = model.addFixedEntity(
            smallPole, smallPole.tileSnappedPosition(tilePos(0, 0)),
        )
        assertEquals(smallPole, placement.prototype)
        assertEquals(pos(0.5, 0.5), placement.position)
        assertEquals("true", placement.selected.toString())
        assertTrue(placement in model.placements)
    }

    @Test
    fun addPlacement() {
        val placement = model.addPlacement(
            smallPole, smallPole.tileSnappedPosition(tilePos(0, 0)),
        )
        assertEquals(1.0, placement.cost)
        assertEquals(smallPole, placement.prototype)
        assertEquals(pos(0.5, 0.5), placement.position)
        assertTrue(
            placement.selected.toString()
                .startsWith("selected_small-electric-pole_0.5_0.5_North")
        )
        assertTrue(placement in model.placements)
    }

    @Test
    fun basicSolve() {
        val placement1 = model.addPlacement(smallPole, smallPole.tileSnappedPosition(tilePos(0, 0)))
        val placement2 = model.addPlacement(smallPole, smallPole.tileSnappedPosition(tilePos(1, 0)))
        placement1.cost = 1.0
        placement2.cost = 2.0
        model.cp.addBoolOr(listOf(placement1.selected, placement2.selected))

        model.timeLimitInSeconds = 1.0
        val solution = model.solve()
        assertTrue(placement1 in solution)
        assertFalse(placement2 in solution)
    }

    @Test
    fun getAllPossiblePlacements() {
        model.addFixedEntity(smallPole, smallPole.tileSnappedPosition(tilePos(0, 0)))
        model.addFixedEntity(smallPole, smallPole.tileSnappedPosition(tilePos(1, 1)))
        val placements = model.getAllPossibleUnrotatedPlacements(
            listOf(smallPole),
            tileBbox(0, 0, 2, 2)
        )
        assertEquals(2, placements.size)
        assertNotNull(placements.singleOrNull { it.position == pos(0.5, 1.5) })
        assertNotNull(placements.singleOrNull { it.position == pos(1.5, 0.5) })
    }
}
