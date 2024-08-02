package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.SignalType
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.SignalIDJson
import glassbricks.factorio.blueprint.prototypes.AccumulatorPrototype
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class AccumulatorTest {
    @Test
    fun `can load accumulator`() {
        val prototype = VanillaPrototypes.getAs<AccumulatorPrototype>("accumulator")
        assertNotNull(prototype.default_output_signal)
        val accumulator = testSaveLoad(Accumulator::class, "accumulator")
        assertEquals(prototype.default_output_signal, accumulator.controlBehavior.defaultOutputSignal)
        assertEquals(prototype.default_output_signal, accumulator.controlBehavior.outputSignal)

        val accumulator2 = testSaveLoad(
            Accumulator::class,
            "accumulator",
            connectToNetwork = true,
            build = fun EntityJson.() {
                control_behavior = ControlBehaviorJson(
                    output_signal = SignalIDJson("signal-B", SignalType.virtual),
                )
            })
        assertEquals(SignalID("signal-B", SignalType.virtual), accumulator2.controlBehavior.outputSignal)
    }
}
