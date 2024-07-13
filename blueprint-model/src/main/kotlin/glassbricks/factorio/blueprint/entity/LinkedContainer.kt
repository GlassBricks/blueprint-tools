package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.prototypes.LinkedContainerPrototype

public class LinkedContainer(
    override val prototype: LinkedContainerPrototype,
    json: EntityJson,
) : BaseEntity(json), WithInventory {
    override var bar: Int? = json.bar
    override val filters: Array<String?> = arrayOfNulls(0)
    public var linkId: Int = json.link_id ?: 0

    override fun exportToJson(json: EntityJson) {
        json.bar = bar
        json.link_id = linkId
    }
}
