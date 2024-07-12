package glassbricks.factorio.blueprint.prototypes

import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Position
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.DoubleArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*


public typealias Position = @Serializable(with = PositionShorthandSerializer::class) Position
public typealias BoundingBox = @Serializable(with = BoundingBoxShorthandSerializer::class) BoundingBox
public typealias ItemOrArray<T> = @Serializable(with = ItemOrArraySerializer::class) List<T>

public object PositionShorthandSerializer : KSerializer<Position> {
    override val descriptor: SerialDescriptor = ListSerializer(Double.serializer()).descriptor

    override fun deserialize(decoder: Decoder): Position {
        decoder as JsonDecoder
        val x: Double
        val y: Double
        when (val element = decoder.decodeJsonElement()) {
            is JsonObject -> {
                x = element["x"]?.jsonPrimitive?.doubleOrNull ?: error("Expected x in position")
                y = element["y"]?.jsonPrimitive?.doubleOrNull ?: error("Expected y in position")
            }

            is JsonArray -> {
                x = element.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: error("Expected x in position tuple")
                y = element.getOrNull(1)?.jsonPrimitive?.doubleOrNull ?: error("Expected y in position tuple")
            }

            else -> throw SerializationException("Unexpected json for Position: $element")
        }
        return Position(x, y)
    }


    override fun serialize(encoder: Encoder, value: Position) {
        encoder.encodeSerializableValue(DoubleArraySerializer(), doubleArrayOf(value.x, value.y))
    }
}

// bounding box: {left_top: Position, right_bottom: Position} or [left_top, right_bottom]

public object BoundingBoxShorthandSerializer : KSerializer<BoundingBox> {
    override val descriptor: SerialDescriptor = ListSerializer(Position.serializer()).descriptor

    override fun deserialize(decoder: Decoder): BoundingBox {
        decoder as JsonDecoder
        val leftTop: JsonObject
        val rightBottom: JsonObject
        when (val element = decoder.decodeJsonElement()) {
            is JsonObject -> {
                leftTop = element["left_top"]?.jsonObject ?: error("Expected left_top in bounding box")
                rightBottom = element["right_bottom"]?.jsonObject ?: error("Expected right_bottom in bounding box")
            }

            is JsonArray -> {
                leftTop = element.getOrNull(0)?.jsonObject ?: error("Expected left_top in bounding box tuple")
                rightBottom = element.getOrNull(1)?.jsonObject ?: error("Expected right_bottom in bounding box tuple")
            }

            else -> throw SerializationException("Unexpected json for BoundingBox: $element")
        }
        return BoundingBox(
            decoder.json.decodeFromJsonElement(PositionShorthandSerializer, leftTop),
            decoder.json.decodeFromJsonElement(PositionShorthandSerializer, rightBottom)
        )
    }

    override fun serialize(encoder: Encoder, value: BoundingBox) {
        encoder.encodeSerializableValue(
            ListSerializer(PositionShorthandSerializer),
            listOf(value.leftTop, value.rightBottom)
        )
    }
}


public class ItemOrArraySerializer<T>(private val itemSerializer: KSerializer<T>) : KSerializer<ItemOrArray<T>> {
    private val listSerializer = ListSerializer(itemSerializer)
    override val descriptor: SerialDescriptor get() = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): ItemOrArray<T> {
        decoder as JsonDecoder
        return when (val element = decoder.decodeJsonElement()) {
            is JsonArray -> decoder.json.decodeFromJsonElement(listSerializer, element)
            else -> listOf(decoder.json.decodeFromJsonElement(itemSerializer, element))
        }
    }

    override fun serialize(encoder: Encoder, value: ItemOrArray<T>) {
        if (value.size == 1) {
            encoder.encodeSerializableValue(itemSerializer, value[0])
        } else {
            encoder.encodeSerializableValue(listSerializer, value)
        }
    }
}
