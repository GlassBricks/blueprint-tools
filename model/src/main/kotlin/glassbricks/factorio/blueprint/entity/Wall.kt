package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.WallPrototype

public class Wall(
    override val prototype: WallPrototype,
    json: EntityJson
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val controlBehavior: WallControlBehavior = WallControlBehavior(json.control_behavior)
    override fun exportToJson(json: EntityJson) {
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): Wall = Wall(prototype, toDummyJson())
}

public class WallControlBehavior(source: ControlBehaviorJson?) : ControlBehavior {
    // todo: make this correct
    public var openCondition: CircuitCondition? = source?.circuit_condition
        ?.takeIf { source.circuit_open_gate == true }
    public var readSensor: Boolean = source?.circuit_read_sensor == true

    override fun exportToJson(): ControlBehaviorJson =
        ControlBehaviorJson(
            circuit_open_gate = openCondition != null,
            circuit_condition = openCondition.takeUnless { it == CircuitCondition.DEFAULT },
            circuit_read_sensor = readSensor
        )
}
