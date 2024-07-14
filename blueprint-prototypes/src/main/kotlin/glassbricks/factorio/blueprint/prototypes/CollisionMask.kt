package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*


/**
 * Every entry in the array is a specification of one layer the object collides with or a special
 * collision option. Supplying an empty table means that no layers and no collision options are set.
 *
 * The default collision masks of all entity types can be found
 * [here](prototype:EntityPrototype::collision_mask). The base game provides common collision mask
 * functions in a Lua file in the core
 * [lualib](https://github.com/wube/factorio-data/blob/master/core/lualib/collision-mask-util.lua).
 *
 * Supplying an empty array means that no layers and no collision options are set.
 *
 * The three options in addition to the standard layers are not collision masks, instead they
 * control other aspects of collision.
 *
 * - `"not-colliding-with-itself"`: Any two entities that both have this option enabled on their prototype and have an identical collision mask layers list will not collide. Other collision mask options are not included in the identical layer list check. This does mean that two different prototypes with the same collision mask layers and this option enabled will not collide.
 * - `"consider-tile-transitions"`: Uses the prototypes position rather than its collision box when doing collision checks with tile prototypes. Allows the prototype to overlap colliding tiles up until its center point. This is only respected for character movement and cars driven by players.
 * - `"colliding-with-tiles-only"`: Any prototype with this collision option will only be checked for collision with other prototype's collision masks if they are a tile.
 */
@Serializable(CollisionMaskSerializer::class)
public data class CollisionMask(
    private val _layers: EnumSet<CollisionMaskLayer>,
    public val notCollidingWithItself: Boolean = false,
    public val considerTileTransitionsOnly: Boolean = false,
    public val collidingWithTilesOnly: Boolean = false,
) {
    public val layers: Set<CollisionMaskLayer> get() = _layers


    /**
     * Returns if this collision mask collides with the other collision mask.
     *
     * Assumes that 2 _entities_ are being tested for collision; tiles are ignored.
     */
    public fun collides(other: CollisionMask): Boolean {
        if (collidingWithTilesOnly || other.collidingWithTilesOnly) return false
        val layerIntersection = _layers.clone().apply { retainAll(other._layers) }
        if (layerIntersection.isEmpty()) return false
        if (notCollidingWithItself && other.notCollidingWithItself && layerIntersection.size == _layers.size) return false
        return true
    }
}

private val stringListSerializer = LuaListSerializer(String.serializer())

internal object CollisionMaskSerializer : KSerializer<CollisionMask> {
    private val layersByName = CollisionMaskLayer.entries.associateBy { it.name }

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor(CollisionMask::class.qualifiedName!!, ListSerializer(String.serializer()).descriptor)

    override fun deserialize(decoder: Decoder): CollisionMask {
        val options = decoder.decodeSerializableValue(stringListSerializer)
        val layers = EnumSet.noneOf(CollisionMaskLayer::class.java)
        var notCollidingWithItself = false
        var considerTileTransitionsOnly = false
        var collidingWithTilesOnly = false
        for (option in options) {
            val layer = layersByName[option]
            if (layer != null) {
                layers.add(layer)
            } else when (option) {
                "not-colliding-with-itself" -> notCollidingWithItself = true
                "consider-tile-transitions-only" -> considerTileTransitionsOnly = true
                "colliding-with-tiles-only" -> collidingWithTilesOnly = true
                else -> throw SerializationException("Unknown collision mask option: $option")
            }
        }
        return CollisionMask(layers, notCollidingWithItself, considerTileTransitionsOnly, collidingWithTilesOnly)
    }

    override fun serialize(encoder: Encoder, value: CollisionMask) {
        val options = mutableListOf<String>()
        options.addAll(value.layers.map { it.name })
        if (value.notCollidingWithItself) options.add("not-colliding-with-itself")
        if (value.considerTileTransitionsOnly) options.add("consider-tile-transitions-only")
        if (value.collidingWithTilesOnly) options.add("colliding-with-tiles-only")
        encoder.encodeSerializableValue(stringListSerializer, options)
    }
}
