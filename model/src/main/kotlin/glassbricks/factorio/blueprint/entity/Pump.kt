package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.PumpPrototype


public class Pump(
    override val prototype: PumpPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val controlBehavior: PumpControlBehavior = PumpControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): Pump = Pump(prototype, jsonForCopy())
}

public class PumpControlBehavior(json: ControlBehaviorJson?) : ControlBehavior {
    public var circuitCondition: CircuitCondition? = json?.circuit_condition

    override fun exportToJson(): ControlBehaviorJson? =
        circuitCondition?.let { ControlBehaviorJson(circuit_condition = it) }
}
