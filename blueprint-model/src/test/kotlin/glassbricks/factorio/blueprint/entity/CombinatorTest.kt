package glassbricks.factorio.blueprint.entity

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
                        signal = SignalID("signal-A", SignalType.Virtual),
                        index = 1,
                    ),
                    ConstantCombinatorParameters(
                        count = 2,
                        signal = SignalID("signal-B", SignalType.Virtual),
                        index = 3,
                    ),
                )
            )
        }
        testSaveLoad<ArithmeticCombinator>("arithmetic-combinator")
        testSaveLoad<ArithmeticCombinator>("arithmetic-combinator") {
            control_behavior = ControlBehaviorJson(
                arithmetic_conditions = ArithmeticCombinatorParameters(
                    first_signal = SignalID("signal-A", SignalType.Virtual),
                    second_constant = 3,
                    operation = ArithmeticOperation.Add,
                    output_signal = SignalID("signal-B", SignalType.Virtual),
                )
            )
        }
        testSaveLoad<DeciderCombinator>("decider-combinator")
        testSaveLoad<DeciderCombinator>("decider-combinator") {
            control_behavior = ControlBehaviorJson(
                decider_conditions = DeciderCombinatorParameters(
                    first_signal = SignalID("signal-A", SignalType.Virtual),
                    constant = 3,
                    comparator = CompareOperation.GreaterOrEqual,
                    output_signal = SignalID("signal-B", SignalType.Virtual),
                )
            )
        }
    }
}
