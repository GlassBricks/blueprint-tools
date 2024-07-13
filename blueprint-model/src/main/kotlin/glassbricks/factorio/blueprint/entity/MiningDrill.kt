package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.MiningDrillResourceReadMode
import glassbricks.factorio.blueprint.prototypes.MiningDrillPrototype


public class MiningDrill(
    public override val prototype: MiningDrillPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectable {
    public override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public override val controlBehavior: MiningDrillControlBehavior = MiningDrillControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class MiningDrillControlBehavior(source: ControlBehaviorJson?) : GenericOnOffControlBehavior(source), ControlBehavior {
    public var resourceReadMode: MiningDrillResourceReadMode? =
        source?.circuit_resource_read_mode?.takeIf { source.circuit_read_resources == true }

    override fun exportToJson(): ControlBehaviorJson = super.baseExportToJson().apply { 
        circuit_enable_disable = circuitCondition != null
        circuit_condition = circuitCondition
        circuit_read_resources = resourceReadMode != null
        circuit_resource_read_mode = resourceReadMode
    }
}
