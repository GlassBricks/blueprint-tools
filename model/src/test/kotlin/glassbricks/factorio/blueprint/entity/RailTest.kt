package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import kotlin.test.Test


class RailTest {
    @Test
    fun `can create rails`() {
        testSaveLoad(StraightRail::class, "straight-rail")
        testSaveLoad(CurvedRail::class, "curved-rail")
        testSaveLoad(CurvedRail::class, "curved-rail") {
            direction = Direction.Northeast
        }
    }
}
