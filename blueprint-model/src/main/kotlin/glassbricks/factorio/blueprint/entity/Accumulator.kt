package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.prototypes.AccumulatorPrototype


public class Accumulator(
    public override val prototype: AccumulatorPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectable {
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public override val controlBehavior: AccumulatorControlBehavior = AccumulatorControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections())
            json.control_behavior = controlBehavior.exportToJson()
    }
}

public class AccumulatorControlBehavior(
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    /** Null will set it to the default value. */
    public var outputSignal: SignalID? = source?.output_signal

    override fun exportToJson(): ControlBehaviorJson = ControlBehaviorJson(output_signal = outputSignal)
}
