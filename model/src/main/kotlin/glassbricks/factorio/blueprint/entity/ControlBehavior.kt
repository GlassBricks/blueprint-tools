package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ControlBehaviorJson

public interface ControlBehavior {
    public fun exportToJson(): ControlBehaviorJson?
}

/**
 * Note that circuit_enable_disable is not always used; subclasses must handle that themselves.
 */
public abstract class GenericOnOffControlBehavior(source: ControlBehaviorJson?) {
    /** If this is not null, sets enable_disable to true. */
    @Suppress("LeakingThis")
    public var circuitCondition: CircuitCondition? =
        if (source != null && shouldHaveCircuitCondition(source)) {
            source.circuit_condition ?: CircuitCondition.DEFAULT
        } else null

    // warning: this leaks `this`, use with caution
    protected open fun shouldHaveCircuitCondition(source: ControlBehaviorJson): Boolean =
        source.circuit_enable_disable == true

    /** If this is not null, sets connectedToLogisticNetwork to true. */
    public var logisticCondition: CircuitCondition? = source?.logistic_condition
        ?.takeIf { source.connect_to_logistic_network }
    public val connectToLogisticNetwork: Boolean get() = logisticCondition != null

    protected fun baseExportToJson(): ControlBehaviorJson = ControlBehaviorJson(
        circuit_condition = circuitCondition.takeUnless { it == CircuitCondition.DEFAULT },
        logistic_condition = logisticCondition,
        connect_to_logistic_network = logisticCondition != null
    )
}
