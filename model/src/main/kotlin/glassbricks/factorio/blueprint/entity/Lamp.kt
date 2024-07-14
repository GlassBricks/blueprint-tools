package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.LampPrototype

public class Lamp(
    override val prototype: LampPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val controlBehavior: LampControlBehavior = LampControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class LampControlBehavior(json: ControlBehaviorJson?) : ControlBehavior {
    public var circuitCondition: CircuitCondition = json?.circuit_condition ?: CircuitCondition.DEFAULT
    public var useColors: Boolean = json?.use_colors ?: false

    override fun exportToJson(): ControlBehaviorJson? =
        if (circuitCondition == CircuitCondition.DEFAULT && !useColors) {
            null
        } else ControlBehaviorJson(
            circuit_condition = circuitCondition.takeUnless { it == CircuitCondition.DEFAULT },
            use_colors = useColors
        )
}
