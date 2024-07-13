package glassbricks.factorio.blueprint.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

internal val bpJson = Json {
    explicitNulls = false
    encodeDefaults = true
}

internal object BlueprintItemSerializer : KSerializer<ImportableBlueprint> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "BlueprintItem",
    ) {
        element("blueprint", Blueprint.serializer().descriptor, isOptional = true)
    }

    override fun deserialize(decoder: Decoder): ImportableBlueprint = decoder.decodeStructure(descriptor) {
        val result = when (decodeElementIndex(descriptor)) {
            0 -> decodeSerializableElement(descriptor, 0, Blueprint.serializer())
            CompositeDecoder.DECODE_DONE -> throw SerializationException("Expected some key, none found")
            CompositeDecoder.UNKNOWN_NAME -> throw SerializationException("Unknown key in root object")
            else -> throw SerializationException("Unexpected index")
        }
        if (decodeElementIndex(descriptor) != CompositeDecoder.DECODE_DONE) {
            throw SerializationException("Expected end of object")
        }
        result
    }


    override fun serialize(encoder: Encoder, value: ImportableBlueprint) = encoder.encodeStructure(descriptor) {
        when (value) {
            is Blueprint -> encodeSerializableElement(descriptor, 0, Blueprint.serializer(), value)
            is BlueprintBook -> TODO()
        }
    }
}

internal object DoubleAsIntSerializer : KSerializer<Double> {
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
