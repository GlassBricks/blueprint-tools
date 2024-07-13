package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.toJsonBasic
import glassbricks.factorio.blueprint.json.toSignalIDBasic
import glassbricks.factorio.blueprint.prototypes.RailChainSignalPrototype
import glassbricks.factorio.blueprint.prototypes.RailSignalBasePrototype
import glassbricks.factorio.blueprint.prototypes.RailSignalPrototype


public sealed class RailSignalBase(
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    abstract override val prototype: RailSignalBasePrototype
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
}


public class RailSignal(
    override val prototype: RailSignalPrototype,
    json: EntityJson,
) : RailSignalBase(json) {
    override val controlBehavior: RailSignalControlBehavior =
        RailSignalControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class RailSignalControlBehavior(
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var readSignal: Boolean = source?.circuit_read_signal ?: false

    // todo: make null mean disabled
    public var redSignal: SignalID? = source?.red_output_signal?.toSignalIDBasic()
    public var orangeSignal: SignalID? = source?.orange_output_signal?.toSignalIDBasic()
    public var greenSignal: SignalID? = source?.green_output_signal?.toSignalIDBasic()

    public var closeSignalCondition: CircuitCondition? = source?.circuit_condition
        ?.takeIf { source.circuit_close_signal == true }

    override fun exportToJson(): ControlBehaviorJson {
        return ControlBehaviorJson(
            circuit_read_signal = readSignal,
            red_output_signal = redSignal.takeIf { readSignal }?.toJsonBasic(),
            orange_output_signal = orangeSignal.takeIf { readSignal }?.toJsonBasic(),
            green_output_signal = greenSignal.takeIf { readSignal }?.toJsonBasic(),
            circuit_close_signal = closeSignalCondition != null,
            circuit_condition = closeSignalCondition,
        )
    }
}

public class RailChainSignal(
    override val prototype: RailChainSignalPrototype,
    json: EntityJson,
) : RailSignalBase(json) {
    override val controlBehavior: RailChainSignalControlBehavior =
        RailChainSignalControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class RailChainSignalControlBehavior(
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var redSignal: SignalID? = source?.red_output_signal?.toSignalIDBasic()
    public var orangeSignal: SignalID? = source?.orange_output_signal?.toSignalIDBasic()
    public var greenSignal: SignalID? = source?.green_output_signal?.toSignalIDBasic()
    public var blueSignal: SignalID? = source?.blue_output_signal?.toSignalIDBasic()

    override fun exportToJson(): ControlBehaviorJson = ControlBehaviorJson(
        red_output_signal = redSignal.toJsonBasic(),
        orange_output_signal = orangeSignal.toJsonBasic(),
        green_output_signal = greenSignal.toJsonBasic(),
        blue_output_signal = blueSignal.toJsonBasic(),
    )
}
