package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.prototypes.ElectricEnergyInterfacePrototype


public class ElectricEnergyInterface(
    override val prototype: ElectricEnergyInterfacePrototype,
    json: EntityJson,
) : BaseEntity(json) {
    public var bufferSize: Long = json.buffer_size ?: 1e9.toLong()
    public var powerProduction: Long = json.power_production ?: 0L
    public var powerUsage: Long = json.power_usage ?: 0L

    override fun exportToJson(json: EntityJson) {
        json.buffer_size = bufferSize
        json.power_production = powerProduction
        json.power_usage = powerUsage
    }
}
