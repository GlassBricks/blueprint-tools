package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.prototypes.LampPrototype

public class Lamp(
    override val prototype: LampPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: LampControlBehavior = LampControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class LampControlBehavior(json: ControlBehaviorJson?) : ControlBehavior {
    public var circuitCondition: CircuitCondition = json?.circuit_condition ?: CircuitCondition.DEFAULT
    public var useColors: Boolean = json?.use_colors ?: false

    override fun exportToJson(): ControlBehaviorJson? {
        if (circuitCondition == CircuitCondition.DEFAULT && !useColors) {
            return null
        }
        return ControlBehaviorJson(
            circuit_condition = circuitCondition,
            use_colors = useColors
        )
    }
}
