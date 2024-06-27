package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ItemPrototypeName


/**
 * If this entity can have item requests.
 * 
 * Currently only includes: crafting machines, locomotives, and beacons.
 */
public interface WithItemRequests {
    public val itemRequests: MutableMap<ItemPrototypeName, Int>
}

internal val EntityInit<WithItemRequests>.itemRequests: MutableMap<ItemPrototypeName, Int>
    get() = self?.itemRequests?.toMutableMap() ?: json?.items?.toMutableMap() ?: mutableMapOf()
