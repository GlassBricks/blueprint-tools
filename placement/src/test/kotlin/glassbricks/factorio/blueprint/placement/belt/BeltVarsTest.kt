package glassbricks.factorio.blueprint.placement.belt

import com.google.ortools.Loader
import com.google.ortools.sat.CpModel
import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.MultiMap
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class BeltVarsTest {
    val config = BeltConfigImpl()
    val cp = CpModel()

    init {
        Loader.loadNativeLibraries()
    }

    @Test
    fun `test cell vars matches config options`() {
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        config.addOption(CardinalDirection.East, BeltType.InputUnderground(ug), 2)
        config.addOption(CardinalDirection.West, BeltType.OutputUnderground(ug), 3)

        val vars = BeltImpl(cp, config)
        val options = vars.getOptions()
        assertEquals<Map<CardinalDirection, MultiMap<BeltType, BeltLineId>>>(
            mapOf(
                CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1)),
                CardinalDirection.East to mapOf(BeltType.InputUnderground(ug) to setOf(2)),
                CardinalDirection.West to mapOf(BeltType.OutputUnderground(ug) to setOf(3))
            ), options
        )
        assertEquals(vars.canBeEmpty, config.canBeEmpty)
        assertEquals(vars.propagatesForward, config.propagatesForward)
        assertEquals(vars.propagatesBackward, config.propagatesBackward)

        val selectedVars = vars.selectedBelt
        assertEquals(setOf(BeltType.Belt(belt)), selectedVars[CardinalDirection.North]!!.keys)
        assertEquals(setOf(BeltType.InputUnderground(ug)), selectedVars[CardinalDirection.East]!!.keys)
        assertEquals(setOf(BeltType.OutputUnderground(ug)), selectedVars[CardinalDirection.West]!!.keys)
        assertFalse(CardinalDirection.South in selectedVars)
    }
}
