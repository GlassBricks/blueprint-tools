package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.InfinityMode
import glassbricks.factorio.blueprint.prototypes.HeatInterfacePrototype


public class HeatInterface(
    override val prototype: HeatInterfacePrototype,
    json: EntityJson,
) : BaseEntity(json) {
    public var temperature: Int = json.temperature ?: 0
    public var mode: InfinityMode = json.mode ?: InfinityMode.AtLeast

    override fun exportToJson(json: EntityJson) {
        json.temperature = temperature
        json.mode = mode
    }

    override fun copyIsolated(): HeatInterface = HeatInterface(prototype, jsonForCopy())
}
