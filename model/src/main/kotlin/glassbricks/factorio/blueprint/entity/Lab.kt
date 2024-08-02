package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.prototypes.LabPrototype


public class Lab(
    override val prototype: LabPrototype,
    json: EntityJson,
) : BaseEntity(json), WithItemRequests {
    override var itemRequests: Map<ItemPrototypeName, Int> = json.items.orEmpty()

    override fun exportToJson(json: EntityJson) {
        if (itemRequests.isNotEmpty()) json.items = itemRequests
    }

    override fun copyIsolated(): Lab = Lab(prototype, toDummyJson())
}
