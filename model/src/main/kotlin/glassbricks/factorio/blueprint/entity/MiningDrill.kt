package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ControlBehaviorJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.json.MiningDrillResourceReadMode
import glassbricks.factorio.blueprint.prototypes.MiningDrillPrototype


public class MiningDrill(
    public override val prototype: MiningDrillPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior, WithItemRequests {
    public override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: MiningDrillControlBehavior = MiningDrillControlBehavior(json.control_behavior)
    override var itemRequests: Map<ItemPrototypeName, Int> = json.items.orEmpty()

    override fun exportToJson(json: EntityJson) {
        if (itemRequests.isNotEmpty()) json.items = itemRequests
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): MiningDrill = MiningDrill(prototype, toDummyJson())
}

public class MiningDrillControlBehavior(json: ControlBehaviorJson?) : GenericOnOffControlBehavior(json),
    ControlBehavior {
    public var resourceReadMode: MiningDrillResourceReadMode? =
        json?.circuit_resource_read_mode?.takeIf { json.circuit_read_resources == true }

    override fun exportToJson(): ControlBehaviorJson = super.baseExportToJson().apply {
        circuit_enable_disable = circuitCondition != null
        circuit_read_resources = resourceReadMode != null
        circuit_resource_read_mode = resourceReadMode
    }
}
