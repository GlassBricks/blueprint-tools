package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.StorageTankPrototype

public class StorageTank(
    override val prototype: StorageTankPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)

    override fun exportToJson(json: EntityJson) {
    }
}
