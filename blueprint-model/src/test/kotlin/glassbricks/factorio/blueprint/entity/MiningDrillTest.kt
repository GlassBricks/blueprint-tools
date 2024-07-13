package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.CompareOperation
import glassbricks.factorio.blueprint.json.MiningDrillResourceReadMode
import org.junit.jupiter.api.Test

class MiningDrillTest {
    @Test
    fun `can load mining drill`() {
        testSaveLoad(MiningDrill::class, "electric-mining-drill")
        testSaveLoad(MiningDrill::class, "electric-mining-drill", connectToNetwork = true, build = fun EntityJson.() {
            control_behavior = ControlBehaviorJson(
                circuit_condition = CircuitCondition(
                    first_signal = signalId("signal-A"),
                    comparator = CompareOperation.Less,
                    constant = 5
                ),
                circuit_enable_disable = true,
                circuit_read_resources = true,
                circuit_resource_read_mode = MiningDrillResourceReadMode.EntirePatch
            )
        })
    }
}
