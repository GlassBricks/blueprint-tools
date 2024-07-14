package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.prototypes.BeaconPrototype


public class Beacon(
    public override val prototype: BeaconPrototype,
    json: EntityJson,
) : BaseEntity(json), WithItemRequests {
    override var itemRequests: Map<ItemPrototypeName, Int> = json.items.orEmpty()

    override fun exportToJson(json: EntityJson) {
        json.items = itemRequests.takeIf { it.isNotEmpty() }
    }

    override fun copyIsolated(): Beacon = Beacon(prototype, toDummyJson())
}
