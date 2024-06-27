package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.prototypes.*


public sealed class CraftingMachine(json: EntityJson) : BaseEntity(json), WithModules, WithEnergySource {
    abstract override val prototype: CraftingMachinePrototype
    override val energySource: EnergySource get() = prototype.energy_source

    override val itemRequests: MutableMap<ItemPrototypeName, Int> = json.items.orEmpty().toMutableMap()

    override fun exportToJson(json: EntityJson) {
        json.items = itemRequests.takeIf { it.isNotEmpty() }
    }
}


public open class AssemblingMachine
internal constructor(
    override val prototype: AssemblingMachinePrototype,
    json: EntityJson,
) : CraftingMachine(json) {
    public var recipe: String? = json.recipe

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.recipe = recipe
    }
}

public class RocketSilo
internal constructor(
    override val prototype: RocketSiloPrototype,
    json: EntityJson,
) : AssemblingMachine(prototype, json) {
    public var autoLaunch: Boolean = json.auto_launch ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.auto_launch = autoLaunch.takeIf { it }
    }
}

public class Furnace
internal constructor(
    override val prototype: FurnacePrototype,
    json: EntityJson,
) : CraftingMachine(json)
