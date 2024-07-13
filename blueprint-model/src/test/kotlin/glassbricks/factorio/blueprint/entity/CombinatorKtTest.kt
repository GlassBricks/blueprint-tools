package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import kotlin.test.Test
import kotlin.test.assertEquals


class CombinatorKtTest {
    @Test
    fun `can save load arithmetic combinators`() {
        val combinator1 = loadEntity<ArithmeticCombinator>("arithmetic-combinator")
        assertEquals(ArithmeticCombinatorParameters.DEFAULT, combinator1.controlBehavior.parameters)

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
    }

    @Test
    fun `can save load decider combinators`() {
        val combinator1 = loadEntity<DeciderCombinator>("decider-combinator")
        assertEquals(DeciderCombinatorParameters.DEFAULT, combinator1.controlBehavior.parameters)

        testSaveLoad<DeciderCombinator>("decider-combinator") {
            control_behavior = ControlBehaviorJson(
                decider_conditions = DeciderCombinatorParameters(
                    first_signal = signalId("signal-A"),
                    second_signal = signalId("signal-B"),
                    comparator = CompareOperation.GreaterOrEqual,
                    output_signal = signalId("signal-B"),
                )
            )
        }
    }

    @Test
    fun `can save load constant combinators`() {
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
    }
}
