package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule


public val DataRawJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    serializersModule = SerializersModule {
        polymorphicDefaultDeserializer(BVEnergySource::class) {
            if (it == null) BurnerEnergySource.serializer() else null
        }
    }
}
