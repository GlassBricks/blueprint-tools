package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Direction
import glassbricks.factorio.blueprint.json.Position
import glassbricks.factorio.blueprint.json.ScheduleRecord
import kotlinx.serialization.json.JsonObject


internal class EntityInit<out T>(
    override val name: String,
    override val position: Position,
    override val direction: Direction,
    override val tags: JsonObject?,
    val json: EntityJson?,
    val originalBlueprint: BlueprintJson?,
    val self: T?,
) : EntityProps

internal fun propInit(
    name: String,
    position: Position,
    direction: Direction,
): EntityInit<Nothing> {
    return EntityInit(name, position, direction, null, null, null, null)
}

internal fun jsonInit(
    json: EntityJson,
    blueprint: BlueprintJson?,
): EntityInit<Nothing> {
    return EntityInit(json.name, json.position, json.direction, json.tags, json, blueprint, null)
}

internal fun <T : EntityProps> copyInit(
    self: T,
): EntityInit<T> {
    return EntityInit(self.name, self.position, self.direction, self.tags, null, null, self)
}


internal fun EntityInit<*>.getSchedule(): List<ScheduleRecord> {
    val json = json
    val originalBlueprint = originalBlueprint
    if (json == null || originalBlueprint == null) return emptyList()
    return originalBlueprint.schedules
        ?.firstOrNull { it.locomotives.any { number -> number == json.entity_number } }
        ?.schedule
        .orEmpty()
}
