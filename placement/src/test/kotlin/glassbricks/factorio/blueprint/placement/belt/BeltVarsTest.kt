package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.Loader
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.beltcp.BeltCpImpl
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class BeltVarsTest {
    val config = BeltConfig(tilePos(0, 0))
    val model = EntityPlacementModel()

    init {
        Loader.loadNativeLibraries()
    }

    @Test
    fun `test cell vars matches config options`() {
        config.addOption(CardinalDirection.North, normalBelt.belt, 1)
        config.addOption(CardinalDirection.East, normalBelt.inputUg, 2)
        config.addOption(CardinalDirection.West, normalBelt.outputUg, 3)

        val vars = BeltCpImpl(model, config)
        val options = vars.options
        assertEquals(
//            multiMapOf(
//                CardinalDirection.North to BeltType.Belt(belt) to 1,
//                CardinalDirection.East to BeltType.InputUnderground(ug) to 2,
//                CardinalDirection.West to BeltType.OutputUnderground(ug) to 3
//            ), options
            setOf(
                BeltOption(CardinalDirection.North, normalBelt.belt, 1),
                BeltOption(CardinalDirection.East, normalBelt.inputUg, 2),
                BeltOption(CardinalDirection.West, normalBelt.outputUg, 3)
            ), options
        )
        assertEquals(vars.canBeEmpty, config.canBeEmpty)
        assertEquals(vars.propagatesForward, config.propagatesForward)
        assertEquals(vars.propagatesBackward, config.propagatesBackward)

        val selectedVars = vars.beltPlacements
        assertEquals(
            setOf(
                CardinalDirection.North to BeltType.Belt(belt),
                CardinalDirection.East to BeltType.InputUnderground(ug),
                CardinalDirection.West to BeltType.OutputUnderground(ug)
            ),
            selectedVars.keys
        )
    }
}
