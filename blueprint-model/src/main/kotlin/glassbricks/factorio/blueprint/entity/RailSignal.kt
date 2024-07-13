package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.toJsonWithDefault
import glassbricks.factorio.blueprint.json.toSignalIdWithDefault
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
        RailSignalControlBehavior(prototype, json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class RailSignalControlBehavior(
    private val prototype: RailSignalPrototype,
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public var readSignal: Boolean = source?.circuit_read_signal ?: false

    public val defaultRedSignal: SignalID? get() = prototype.default_red_output_signal
    public val defaultOrangeSignal: SignalID? get() = prototype.default_orange_output_signal
    public val defaultGreenSignal: SignalID? get() = prototype.default_green_output_signal

    public var redSignal: SignalID? = source?.red_output_signal.toSignalIdWithDefault(defaultRedSignal)
    public var orangeSignal: SignalID? = source?.orange_output_signal.toSignalIdWithDefault(defaultOrangeSignal)
    public var greenSignal: SignalID? = source?.green_output_signal.toSignalIdWithDefault(defaultGreenSignal)

    public var closeSignalCondition: CircuitCondition? = source?.circuit_condition
        ?.takeIf { source.circuit_close_signal == true }

    override fun exportToJson(): ControlBehaviorJson {
        return ControlBehaviorJson(
            circuit_read_signal = readSignal,
            red_output_signal = redSignal.toJsonWithDefault(defaultRedSignal).takeIf { readSignal },
            orange_output_signal = orangeSignal.toJsonWithDefault(defaultOrangeSignal).takeIf { readSignal },
            green_output_signal = greenSignal.toJsonWithDefault(defaultGreenSignal).takeIf { readSignal },
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
        RailChainSignalControlBehavior(prototype, json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class RailChainSignalControlBehavior(
    prototype: RailChainSignalPrototype,
    source: ControlBehaviorJson? = null,
) : ControlBehavior {
    public val defaultRedSignal: SignalID? = prototype.default_red_output_signal
    public val defaultOrangeSignal: SignalID? = prototype.default_orange_output_signal
    public val defaultGreenSignal: SignalID? = prototype.default_green_output_signal
    public val defaultBlueSignal: SignalID? = prototype.default_blue_output_signal

    public var redSignal: SignalID? = source?.red_output_signal.toSignalIdWithDefault(defaultRedSignal)
    public var orangeSignal: SignalID? = source?.orange_output_signal.toSignalIdWithDefault(defaultOrangeSignal)
    public var greenSignal: SignalID? = source?.green_output_signal.toSignalIdWithDefault(defaultGreenSignal)
    public var blueSignal: SignalID? = source?.blue_output_signal.toSignalIdWithDefault(defaultBlueSignal)

    override fun exportToJson(): ControlBehaviorJson? = if (redSignal == defaultRedSignal &&
        orangeSignal == defaultOrangeSignal &&
        greenSignal == defaultGreenSignal &&
        blueSignal == defaultBlueSignal
    ) {
        null
    } else ControlBehaviorJson(
        red_output_signal = redSignal.toJsonWithDefault(defaultRedSignal),
        orange_output_signal = orangeSignal.toJsonWithDefault(defaultOrangeSignal),
        green_output_signal = greenSignal.toJsonWithDefault(defaultGreenSignal),
        blue_output_signal = blueSignal.toJsonWithDefault(defaultBlueSignal),
    )
}
