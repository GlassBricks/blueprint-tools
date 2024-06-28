package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.prototypes.AssemblingMachinePrototype
import glassbricks.factorio.prototypes.CraftingMachinePrototype
import glassbricks.factorio.prototypes.FurnacePrototype
import glassbricks.factorio.prototypes.RocketSiloPrototype


public sealed class CraftingMachine(json: EntityJson) : BaseEntity(json), WithModules {
    abstract override val prototype: CraftingMachinePrototype

    override val itemRequests: MutableMap<ItemPrototypeName, Int> = json.items.orEmpty().toMutableMap()

    override fun exportToJson(json: EntityJson) {
        json.items = itemRequests.takeIf { it.isNotEmpty() }
    }
}


public open class AssemblingMachine(
    override val prototype: AssemblingMachinePrototype,
    json: EntityJson,
) : CraftingMachine(json) {
    public var recipe: String? = json.recipe

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.recipe = recipe
    }
}

public class RocketSilo(
    override val prototype: RocketSiloPrototype,
    json: EntityJson,
) : AssemblingMachine(prototype, json) {
    public var autoLaunch: Boolean = json.auto_launch ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.auto_launch = autoLaunch.takeIf { it }
    }
}

public class Furnace(
    override val prototype: FurnacePrototype,
    json: EntityJson,
) : CraftingMachine(json)
