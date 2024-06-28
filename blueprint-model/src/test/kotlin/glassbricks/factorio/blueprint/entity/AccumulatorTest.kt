package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.json.SignalType
import kotlin.test.Test


class AccumulatorTest  {
    @Test
    fun `can load accumulator` () {
        testSaveLoad<Accumulator>("accumulator")
        testSaveLoad<Accumulator>("accumulator", connectToNetwork = true) {
            control_behavior = ControlBehaviorJson(
                output_signal = SignalID("signal-A", SignalType.Virtual),
            )
        }
    }
}
