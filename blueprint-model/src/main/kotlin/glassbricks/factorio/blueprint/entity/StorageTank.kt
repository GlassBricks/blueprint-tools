package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.prototypes.StorageTankPrototype

public class StorageTank(
    override val prototype: StorageTankPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectable {
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    override val controlBehavior: Nothing? get() = null

    override fun exportToJson(json: EntityJson) {
    }
}
