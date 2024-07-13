package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.SignalType
import glassbricks.factorio.blueprint.json.SignalIDJson
import glassbricks.factorio.blueprint.prototypes.AccumulatorPrototype
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class AccumulatorTest {
    @Test
    fun `can load accumulator`() {
        val prototype = blueprintPrototypes.getPrototype<AccumulatorPrototype>("accumulator")
        assertNotNull(prototype.default_output_signal)
        val accumulator = testSaveLoad<Accumulator>("accumulator")
        assertEquals(prototype.default_output_signal, accumulator.controlBehavior.defaultOutputSignal)
        assertEquals(prototype.default_output_signal, accumulator.controlBehavior.outputSignal)

        val accumulator2 = testSaveLoad<Accumulator>("accumulator", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                output_signal = SignalIDJson("signal-B", SignalType.virtual),
            )
        }
        assertEquals(SignalID("signal-B", SignalType.virtual), accumulator2.controlBehavior.outputSignal)
    }
}
