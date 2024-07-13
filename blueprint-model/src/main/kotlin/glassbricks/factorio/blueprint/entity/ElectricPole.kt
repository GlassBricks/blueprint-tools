package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype

public class ElectricPole(
    override val prototype: ElectricPolePrototype,
    json: EntityJson,
) : BaseEntity(json), CableConnectionPoint, CircuitConnectionPoint {
    override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    override val circuitConnections: CircuitConnections = CircuitConnections(this)

    override val entity: Entity get() = this

    override fun exportToJson(json: EntityJson) {
        // all connections handled by ImportExport
    }
}
