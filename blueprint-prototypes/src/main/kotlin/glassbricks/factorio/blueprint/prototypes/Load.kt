package glassbricks.factorio.blueprint.prototypes

import kotlinx.serialization.json.Json


public val DataRawJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    useAlternativeNames = false
}
