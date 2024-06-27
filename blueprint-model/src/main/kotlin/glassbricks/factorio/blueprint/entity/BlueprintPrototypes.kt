package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.prototypes.*
import java.io.File
import java.io.InputStream


/**
 * @property placeableBy A map of entity names to the item that can place them.
 * If an entity requires multiple items to be placed (via placeable_by), this only contains the first item. Will be present for every entity in [prototypes].
 */
public class BlueprintPrototypes(
    public val prototypes: Map<String, EntityWithOwnerPrototype>,
    public val placeableBy: Map<String, String>,
) {

    public companion object {
        public fun fromFactorioPrototypes(prototypes: PrototypeData): BlueprintPrototypes =
            toBlueprintPrototypes(prototypes)

        public fun fromDataRawFile(stream: InputStream): BlueprintPrototypes =
            toBlueprintPrototypes(loadFactorioPrototypesFromStream(stream))

        public fun fromDataRawFile(file: File): BlueprintPrototypes =
            fromDataRawFile(file.inputStream())
    }
}

private fun isBlueprintablePrototype(prototype: Any): Boolean =
    prototype is EntityWithOwnerPrototype && prototype !is UnitPrototype &&
            !(prototype is VehiclePrototype && prototype !is RollingStockPrototype)

private fun toBlueprintPrototypes(prototypeData: PrototypeData): BlueprintPrototypes {
    val placeableBy = hashMapOf<String, String>()


    val entityPrototypeMaps = mutableListOf<Map<String, EntityWithOwnerPrototype>>()

    @Suppress("UNCHECKED_CAST")
    for (key in prototypeData.keys) {
        val protoMap = prototypeData[key] as? Map<String, Any>
        if (protoMap.isNullOrEmpty()) continue
        val firstProto = protoMap.values.first()
        if (isBlueprintablePrototype(firstProto)) {
            entityPrototypeMaps.add(protoMap as Map<String, EntityWithOwnerPrototype>)
        } else if (firstProto is ItemPrototype) {
            for ((name, prototype) in protoMap as Map<String, ItemPrototype>) {
                val placeResult = prototype.place_result ?: continue
                if (placeResult !in placeableBy || prototype.flags?.contains(ItemPrototypeFlag.`primary-place-result`) == true) {
                    placeableBy[placeResult] = name
                }
            }
        }
    }
    val prototypeMap = hashMapOf<String, EntityWithOwnerPrototype>()
    for (entityPrototypeMap in entityPrototypeMaps) {
        for ((name, prototype) in entityPrototypeMap) {
            val flags = prototype.flags ?: continue
            if (EntityPrototypeFlag.`player-creation` !in flags || EntityPrototypeFlag.`not-blueprintable` in flags) continue
            val itemsToPlace = prototype.placeable_by
            if (itemsToPlace != null) {
                placeableBy[name] = itemsToPlace.first().item
            }
            if (name !in placeableBy) continue
            prototypeMap[name] = prototype
        }
    }
    placeableBy.keys.retainAll(prototypeMap.keys)

    return BlueprintPrototypes(prototypeMap, placeableBy)
}
