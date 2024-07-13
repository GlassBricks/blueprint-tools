package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.prototypes.LinkedContainerPrototype

public class LinkedContainer(
    override val prototype: LinkedContainerPrototype,
    json: EntityJson,
) : BaseEntity(json), WithBar {
    override var bar: Int? = json.bar
    public var link_id: Int = json.link_id ?: 0

    override fun exportToJson(json: EntityJson) {
        json.bar = bar
        json.link_id = link_id
    }
}
