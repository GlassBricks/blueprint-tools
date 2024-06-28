package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.prototypes.SimpleEntityWithOwnerPrototype

/**
 * Also includes SimpleEntityWithForcePrototype (a subclass).
 */
public class SimpleEntityWithOwner(
    override val prototype: SimpleEntityWithOwnerPrototype,
    json: EntityJson,
) : BaseEntity(json) {
    public var variation: Int = json.variation?.toInt() ?: 1

    override fun exportToJson(json: EntityJson) {
        json.variation = variation.toUByte()
    }
}
