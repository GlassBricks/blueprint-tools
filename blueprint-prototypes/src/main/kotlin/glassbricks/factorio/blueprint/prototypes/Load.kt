package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.json.Json


public val dataRawJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
