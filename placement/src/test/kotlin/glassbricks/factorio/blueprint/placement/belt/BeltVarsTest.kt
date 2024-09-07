package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.Loader
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.EntityPlacementModel
import glassbricks.factorio.blueprint.placement.multiMapOf
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class BeltVarsTest {
    val config = BeltConfigImpl(tilePos(0, 0))
    val model = EntityPlacementModel()

    init {
        Loader.loadNativeLibraries()
    }

    @Test
    fun `test cell vars matches config options`() {
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        config.addOption(CardinalDirection.East, BeltType.InputUnderground(ug), 2)
        config.addOption(CardinalDirection.West, BeltType.OutputUnderground(ug), 3)

        val vars = BeltImpl(model, config)
        val options = vars.getOptions()
        assertEquals(
            multiMapOf(
                CardinalDirection.North to BeltType.Belt(belt) to 1,
                CardinalDirection.East to BeltType.InputUnderground(ug) to 2,
                CardinalDirection.West to BeltType.OutputUnderground(ug) to 3
            ), options
        )
        assertEquals(vars.canBeEmpty, config.canBeEmpty)
        assertEquals(vars.propagatesForward, config.propagatesForward)
        assertEquals(vars.propagatesBackward, config.propagatesBackward)

        val selectedVars = vars.selectedBelt
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
