package glassbricks.factorio.blueprint.placement.grid

import com.google.ortools.Loader
import com.google.ortools.sat.CpModel
import glassbricks.factorio.blueprint.placement.MultiMap
import glassbricks.factorio.blueprint.prototypes.TransportBeltPrototype
import glassbricks.factorio.blueprint.prototypes.UndergroundBeltPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val belt get() = VanillaPrototypes.getAs<TransportBeltPrototype>("transport-belt")
private val ug get() = VanillaPrototypes.getAs<UndergroundBeltPrototype>("underground-belt")

class CellBeltConfigTest {
    val cell = CellConfig().belt

    @Test
    fun testCanAddOption() {
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        cell.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        val options = cell.getOptions()
        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
            mapOf(
                CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1)),
                CardinalDirection.East to mapOf(BeltType.Belt(belt) to setOf(2))
            ), options
        )
    }

    @Test
    fun testMakeLineStart() {
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 2)
        cell.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        cell.makeLineStart(CardinalDirection.North, 1)

        val options = cell.getOptions()
        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
            mapOf(CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1))), options
        )
        assertTrue(cell.propagatesIdForward)
        assertFalse(cell.propagatesIdBackward)
        assertFalse(cell.canBeEmpty)
    }

    @Test
    fun testMakeLineEnd() {
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 2)
        cell.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        cell.makeLineEnd(CardinalDirection.North, 1)

        val options = cell.getOptions()
        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
            mapOf(CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1))), options
        )
        assertFalse(cell.propagatesIdForward)
        assertTrue(cell.propagatesIdBackward)
        assertFalse(cell.canBeEmpty)
    }

    @Test
    fun testCanMakeLineStartEnd() {
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        cell.addOption(CardinalDirection.North, BeltType.Belt(belt), 2)
        cell.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        cell.makeLineStart(CardinalDirection.North, 1)
        cell.makeLineEnd(CardinalDirection.North, 1)

        val options = cell.getOptions()
        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
            mapOf(CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1))), options
        )

        assertFalse(cell.propagatesIdForward)
        assertFalse(cell.propagatesIdBackward)
        assertFalse(cell.canBeEmpty)
    }

}

class CellBeltVarsTest {
    val cell = CellConfig()
    val cp = CpModel()

    init {
        Loader.loadNativeLibraries()
    }

    @Test
    fun `test cell vars matches config options`() {
        cell.belt.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        cell.belt.addOption(CardinalDirection.East, BeltType.InputUnderground(ug), 2)
        cell.belt.addOption(CardinalDirection.West, BeltType.OutputUnderground(ug), 3)

        val vars = CellVarsImpl(cp, cell)
        val options = vars.getOptions()
        assertEquals<Map<CardinalDirection, MultiMap<BeltType, BeltLineId>>>(
            mapOf(
                CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1)),
                CardinalDirection.East to mapOf(BeltType.InputUnderground(ug) to setOf(2)),
                CardinalDirection.West to mapOf(BeltType.OutputUnderground(ug) to setOf(3))
            ), options
        )
        assertEquals(vars.canBeEmpty, cell.belt.canBeEmpty)
        assertEquals(vars.propagatesIdForward, cell.belt.propagatesIdForward)
        assertEquals(vars.propagatesIdBackward, cell.belt.propagatesIdBackward)

        val selectedVars = vars.selectVars
        assertEquals(setOf(BeltType.Belt(belt)), selectedVars[CardinalDirection.North]!!.keys)
        assertEquals(setOf(BeltType.InputUnderground(ug)), selectedVars[CardinalDirection.East]!!.keys)
        assertEquals(setOf(BeltType.OutputUnderground(ug)), selectedVars[CardinalDirection.West]!!.keys)
        assertFalse(CardinalDirection.South in selectedVars)
    }
}
