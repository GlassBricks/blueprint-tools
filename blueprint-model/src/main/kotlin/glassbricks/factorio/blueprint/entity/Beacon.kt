package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.prototypes.BeaconPrototype


public class Beacon(
    public override val prototype: BeaconPrototype,
    json: EntityJson,
) : BaseEntity(json), WithItemRequests {
    override val itemRequests: MutableMap<ItemPrototypeName, Int> = json.items?.toMutableMap() ?: mutableMapOf()

    override fun exportToJson(json: EntityJson) {
        json.items = itemRequests.takeIf { it.isNotEmpty() }
    }
}
