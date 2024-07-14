package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.toJsonWithDefault
import glassbricks.factorio.blueprint.json.toSignalIdWithDefault
import glassbricks.factorio.blueprint.prototypes.AccumulatorPrototype


public class Accumulator(
    public override val prototype: AccumulatorPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: AccumulatorControlBehavior = AccumulatorControlBehavior(
        prototype,
        json.control_behavior
    )

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections())
            json.control_behavior = controlBehavior.exportToJson()
    }
}

public class AccumulatorControlBehavior(
    private val prototype: AccumulatorPrototype,
    source: ControlBehaviorJson?
) : ControlBehavior {
    public val defaultOutputSignal: SignalID? get() = prototype.default_output_signal
    public var outputSignal: SignalID? = source?.output_signal.toSignalIdWithDefault(defaultOutputSignal)

    override fun exportToJson(): ControlBehaviorJson? {
        if (outputSignal == defaultOutputSignal) return null
        return ControlBehaviorJson(output_signal = outputSignal.toJsonWithDefault(defaultOutputSignal))
    }
}
