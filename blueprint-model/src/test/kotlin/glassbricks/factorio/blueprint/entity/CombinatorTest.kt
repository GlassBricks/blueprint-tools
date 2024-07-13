package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalType
import glassbricks.factorio.blueprint.json.*
import kotlin.test.Test


class CombinatorTest {
    @Test
    fun `can load combinators`() {
        testSaveLoad<ConstantCombinator>("constant-combinator")
        testSaveLoad<ConstantCombinator>("constant-combinator") {
            control_behavior = ControlBehaviorJson(
                is_on = false,
                filters = listOf(
                    ConstantCombinatorParameters(
                        count = 1,
                        signal = signalId("signal-A"),
                        index = 1,
                    ),
                    ConstantCombinatorParameters(
                        count = 2,
                        signal = signalId("signal-B"),
                        index = 3,
                    ),
                )
            )
        }
//        testSaveLoad<ArithmeticCombinator>("arithmetic-combinator")
        testSaveLoad<ArithmeticCombinator>("arithmetic-combinator") {
            control_behavior = ControlBehaviorJson(
                arithmetic_conditions = ArithmeticCombinatorParameters(
                    first_signal = signalId("signal-A"),
                    second_constant = 3,
                    operation = ArithmeticOperation.Add,
                    output_signal = signalId("signal-B"),
                )
            )
        }
//        testSaveLoad<DeciderCombinator>("decider-combinator")
        testSaveLoad<DeciderCombinator>("decider-combinator") {
            control_behavior = ControlBehaviorJson(
                decider_conditions = DeciderCombinatorParameters(
                    first_signal = signalId("signal-A"),
                    constant = 3,
                    comparator = CompareOperation.GreaterOrEqual,
                    output_signal = signalId("signal-B"),
                )
            )
        }
    }
}
