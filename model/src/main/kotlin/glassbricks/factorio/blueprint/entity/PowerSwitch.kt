package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.PowerSwitchPrototype

public class PowerSwitch(
    override val prototype: PowerSwitchPrototype,
    json: EntityJson,
) : BaseEntity(json), PowerSwitchConnectionPoints, CircuitConnectionPoint, WithControlBehavior {
    public override val left: CableConnectionPoint = ConnectionPoint()
    public override val right: CableConnectionPoint = ConnectionPoint()
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val controlBehavior: PowerSwitchControlBehavior = PowerSwitchControlBehavior(json.control_behavior)

    public var switchState: Boolean = json.switch_state ?: true

    override fun exportToJson(json: EntityJson) {
        json.switch_state = switchState
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }

    private inner class ConnectionPoint : CableConnectionPoint {
        override val entity: PowerSwitch get() = this@PowerSwitch
        override val cableConnections: CableConnections = CableConnections(this)
    }

    override fun copyIsolated(): PowerSwitch = PowerSwitch(prototype, toDummyJson())
}

public class PowerSwitchControlBehavior(
    source: ControlBehaviorJson?,
) : ControlBehavior {
    public var circuitCondition: CircuitCondition = source?.circuit_condition ?: CircuitCondition.DEFAULT

    override fun exportToJson(): ControlBehaviorJson? =
        if (circuitCondition == CircuitCondition.DEFAULT) {
            null
        } else {
            ControlBehaviorJson(circuit_condition = circuitCondition)
        }
}
