package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition

public sealed interface ControlBehavior {
    public fun exportToJson(): ControlBehaviorJson?
    public fun copy(): ControlBehavior
}

/**
 * Note that circuit_enable_disable is not always used; subclasses must handle that themselves.
 */
public abstract class GenericOnOffControlBehavior(
    source: ControlBehaviorJson?,
) {
    /** If this is not null, sets enable_disable to true. */
    public var circuitCondition: CircuitCondition? = source?.circuit_condition?.copy()
        ?.takeIf { source.circuit_enable_disable == true }

    /** If this is not null, sets connectedToLogisticNetwork to true. */
    public var logisticCondition: CircuitCondition? = source?.logistic_condition?.copy()
        ?.takeIf { source.connect_to_logistic_network }
    public val connectToLogisticNetwork: Boolean get() = logisticCondition != null

    protected open fun exportToJson(): ControlBehaviorJson? {
        if(circuitCondition == null && logisticCondition == null) return null
        return ControlBehaviorJson(
            circuit_condition = circuitCondition,
            logistic_condition = logisticCondition,
            connect_to_logistic_network = logisticCondition != null
        )
    }
    protected fun copyTo(target: GenericOnOffControlBehavior) {
        target.circuitCondition = circuitCondition?.copy()
        target.logisticCondition = logisticCondition?.copy()
    }
}

// todo:
// container
// generic_on_off
// inserter
// lamp
// logistic_container
// roboport
// storage_tank
// train_stop
// decider_combinator
// arithmetic_combinator
// constant_combinator
// transport_belt
// accumulator
// rail_signal
// rail_chain_signal
// wall
// mining_drill
// programmable_speaker
