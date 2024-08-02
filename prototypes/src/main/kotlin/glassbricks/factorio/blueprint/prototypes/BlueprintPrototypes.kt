package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.InputStream
import java.net.URL


/**
 * Contains information about all blueprint-able prototypes. More convenient info loaded from [DataRaw].
 */
public class BlueprintPrototypes(public val dataRaw: DataRaw) {
    /** Contains _all_ items */
    public val items: Map<ItemID, ItemPrototype> = dataRaw.allItemPrototypes().associateBy { it.name }

    /** Only contains blueprintable entities */
    public val blueprintableEntities: Map<EntityID, EntityWithOwnerPrototype>

    /** Holds cost to place 1 of an entity. Only contains keys also present in [blueprintableEntities] */
    public val itemsToPlace: Map<EntityID, List<ItemToPlace>>

    public inline fun <reified T : EntityWithOwnerPrototype> getAs(name: String): T = blueprintableEntities[name] as T
    public operator fun get(name: String): EntityWithOwnerPrototype? = blueprintableEntities[name]

    init {
        val itemsToPlace = mutableMapOf<EntityID, List<ItemToPlace>>()
        for ((_, itemPrototype) in items) {
            val entityId = itemPrototype.place_result
            if (entityId !in itemsToPlace
                || itemPrototype.flags?.contains(ItemPrototypeFlag.`primary-place-result`) == true
            ) {
                itemsToPlace[entityId] = listOf(ItemToPlace(item = itemPrototype.name, count = 1u))
            }
        }
        val blueprintableEntities = mutableMapOf<EntityID, EntityWithOwnerPrototype>()
        for (entityPrototype in dataRaw.allEntityWithOwnerPrototypes()) {
            // entity is blueprintable if:
            // has "player-creation" flag, does not have "not-blueprintable" flag
            // can be placed by something
            val name = entityPrototype.name
            val flags = entityPrototype.flags ?: continue
            if (EntityPrototypeFlag.`player-creation` !in flags || EntityPrototypeFlag.`not-blueprintable` in flags) continue
            val placeableBy = entityPrototype.placeable_by
            if (placeableBy != null) {
                itemsToPlace[name] = placeableBy
            }

            if (name !in itemsToPlace) continue
            blueprintableEntities[name] = entityPrototype
        }

        itemsToPlace.keys.retainAll(blueprintableEntities.keys)

        this.itemsToPlace = itemsToPlace
        this.blueprintableEntities = blueprintableEntities
    }

    public companion object {
        @OptIn(ExperimentalSerializationApi::class)
        public fun loadFromDataRaw(stream: InputStream): BlueprintPrototypes {
            val dataRaw = stream.use {
                DataRawJson.decodeFromStream(DataRaw.serializer(), it)
            }
            return BlueprintPrototypes(dataRaw)
        }

        public fun loadFromDataRaw(file: File): BlueprintPrototypes = loadFromDataRaw(file.inputStream())
        public fun loadFromDataRaw(uri: URL): BlueprintPrototypes = loadFromDataRaw(uri.openStream())

        public fun loadFromDataRaw(content: String): BlueprintPrototypes {
            val dataRaw = DataRawJson.decodeFromString(DataRaw.serializer(), content)
            return BlueprintPrototypes(dataRaw)
        }
    }
}
