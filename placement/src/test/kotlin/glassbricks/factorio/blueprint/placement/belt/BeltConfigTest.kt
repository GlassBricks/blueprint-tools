package glassbricks.factorio.blueprint.placement.belt

import glassbricks.factorio.blueprint.placement.CardinalDirection
import glassbricks.factorio.blueprint.placement.multiTableOf
import glassbricks.factorio.blueprint.tilePos
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BeltConfigTest {
    val config = BeltConfigImpl(tilePos(0, 0))

    @Test
    fun testCantAddId0() {
        assertThrows<IllegalArgumentException> {
            config.addOption(CardinalDirection.North, BeltType.Belt(belt), 0)
        }
    }

    @Test
    fun testCanAddOption() {
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        config.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        val options = config.getOptions()
//        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
//            mapOf(
//                CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1)),
//                CardinalDirection.East to mapOf(BeltType.Belt(belt) to setOf(2))
//            ), options
//        )
        assertEquals(
            multiTableOf<CardinalDirection, BeltType, BeltLineId>(
                (CardinalDirection.North to normalBelt.belt) to 1,
                (CardinalDirection.East to normalBelt.belt) to 2
            ), options
        )
    }

    @Test
    fun testMakeLineStart() {
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 2)
        config.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        config.makeLineStart(CardinalDirection.North, 1)

        val options = config.getOptions()
        assertEquals(
            multiTableOf<CardinalDirection, BeltType, BeltLineId>(
                (CardinalDirection.North to normalBelt.belt) to 1
            ), options
        )
        assertTrue(config.propagatesForward)
        assertFalse(config.propagatesBackward)
        assertFalse(config.canBeEmpty)
    }

    @Test
    fun testMakeLineEnd() {
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 2)
        config.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        config.makeLineEnd(CardinalDirection.North, 1)

        val options = config.getOptions()
//        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
//            mapOf(CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1))), options
//        )
        assertEquals(
            multiTableOf<CardinalDirection, BeltType, BeltLineId>(
                (CardinalDirection.North to normalBelt.belt) to 1
            ), options
        )
        assertFalse(config.propagatesForward)
        assertTrue(config.propagatesBackward)
        assertFalse(config.canBeEmpty)
    }

    @Test
    fun testCanMakeLineStartEnd() {
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 1)
        config.addOption(CardinalDirection.North, BeltType.Belt(belt), 2)
        config.addOption(CardinalDirection.East, BeltType.Belt(belt), 2)

        config.makeLineStart(CardinalDirection.North, 1)
        config.makeLineEnd(CardinalDirection.North, 1)

        val options = config.getOptions()
//        assertEquals<Map<CardinalDirection, Map<out BeltType, Set<BeltLineId>>>>(
//            mapOf(CardinalDirection.North to mapOf(BeltType.Belt(belt) to setOf(1))), options
//        )
        assertEquals(
            multiTableOf<CardinalDirection, BeltType, BeltLineId>(
                (CardinalDirection.North to normalBelt.belt) to 1
            ), options
        )

        assertFalse(config.propagatesForward)
        assertFalse(config.propagatesBackward)
        assertFalse(config.canBeEmpty)
    }

}
