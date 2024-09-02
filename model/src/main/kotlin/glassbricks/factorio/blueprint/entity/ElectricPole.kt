package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype

public class ElectricPole(
    override val prototype: ElectricPolePrototype,
    json: EntityJson,
) : BaseEntity(json), CableConnectionPoint, CircuitConnectionPoint {
    override val cableConnections: CableConnections = CableConnections(this)
    override val circuitConnections: CircuitConnections = CircuitConnections(this)

    override val entity: BlueprintEntity get() = this

    override fun exportToJson(json: EntityJson) {
        // all connections handled by ImportExport
    }

    override fun copyIsolated(): ElectricPole = ElectricPole(prototype, jsonForCopy())
}
