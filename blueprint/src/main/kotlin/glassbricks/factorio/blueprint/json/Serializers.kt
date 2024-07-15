package glassbricks.factorio.blueprint.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal val bpJson = Json {
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
private class BlueprintProxy(
    val blueprint: BlueprintJson? = null,
    val blueprint_book: BlueprintBookJson? = null,
)

internal object ImportableSerializer : KSerializer<Importable> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor(
        Importable::class.qualifiedName!!,
        BlueprintProxy.serializer().descriptor
    )

    override fun deserialize(decoder: Decoder): Importable {
        val proxy = decoder.decodeSerializableValue(BlueprintProxy.serializer())
        return proxy.blueprint ?: proxy.blueprint_book!!
    }


    override fun serialize(encoder: Encoder, value: Importable) {
        val proxy = when (value) {
            is BlueprintJson -> BlueprintProxy(blueprint = value)
            is BlueprintBookJson -> BlueprintProxy(blueprint_book = value)
        }
        encoder.encodeSerializableValue(BlueprintProxy.serializer(), proxy)
    }
}

@Serializable
private class BlueprintIndexProxy(
    val index: Int,
    val blueprint: BlueprintJson? = null,
    val blueprint_book: BlueprintBookJson? = null,
)

internal object BlueprintIndexSerializer : KSerializer<BlueprintIndex> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor(
        BlueprintIndex::class.qualifiedName!!,
        BlueprintIndexProxy.serializer().descriptor
    )

    override fun deserialize(decoder: Decoder): BlueprintIndex {
        val proxy = decoder.decodeSerializableValue(BlueprintIndexProxy.serializer())
        val item = proxy.blueprint ?: proxy.blueprint_book!!
        return BlueprintIndex(index = proxy.index, item = item)
    }

    override fun serialize(encoder: Encoder, value: BlueprintIndex) {
        val proxy = when (val item = value.item) {
            is BlueprintJson -> BlueprintIndexProxy(index = value.index, blueprint = item)
            is BlueprintBookJson -> BlueprintIndexProxy(index = value.index, blueprint_book = item)
        }
        encoder.encodeSerializableValue(BlueprintIndexProxy.serializer(), proxy)
    }
}

public object DoubleAsIntSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DoubleAsInt", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
    override fun serialize(encoder: Encoder, value: Double) {
        val asLong = value.toLong()
        if (asLong.toDouble() == value) {
            encoder.encodeLong(asLong)
        } else {
            encoder.encodeDouble(value)
        }
    }
}

/** A double that is serialized as an integer if it can be represented as such. */
internal typealias DoubleAsInt = @Serializable(with = DoubleAsIntSerializer::class) Double

internal fun getSerialName(
    clazz: KClass<*>,
): String {
    return (clazz.java.getDeclaredAnnotation(SerialName::class.java))?.value ?: clazz.qualifiedName!!
}

internal open class EnumOrdinalSerializer<T : Enum<T>>(
    clazz: KClass<T>,
    private val offset: Int = 0
) : KSerializer<T> {
    private val values: Array<out T> = clazz.java.enumConstants
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(getSerialName(clazz), PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(value.ordinal + offset)
    override fun deserialize(decoder: Decoder): T = values[decoder.decodeInt() - offset]
}
