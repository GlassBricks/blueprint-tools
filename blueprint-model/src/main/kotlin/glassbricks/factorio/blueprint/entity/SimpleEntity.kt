package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.SimpleEntityWithForcePrototype
import glassbricks.factorio.blueprint.prototypes.SimpleEntityWithOwnerPrototype

/**
 * Also includes SimpleEntityWithForce, since [SimpleEntityWithForcePrototype] is a subclass of [SimpleEntityWithOwnerPrototype].
 */
public class SimpleEntityWithOwner(
    override val prototype: SimpleEntityWithOwnerPrototype,
    json: EntityJson,
) : BaseEntity(json) {
    public var variation: UByte = json.variation ?: 0u

    override fun exportToJson(json: EntityJson) {
        json.variation = variation
    }
}
