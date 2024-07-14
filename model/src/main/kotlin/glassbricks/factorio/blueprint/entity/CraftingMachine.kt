package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.prototypes.AssemblingMachinePrototype
import glassbricks.factorio.blueprint.prototypes.CraftingMachinePrototype
import glassbricks.factorio.blueprint.prototypes.FurnacePrototype
import glassbricks.factorio.blueprint.prototypes.RocketSiloPrototype


public sealed class CraftingMachine(json: EntityJson) : BaseEntity(json), WithItemRequests {
    abstract override val prototype: CraftingMachinePrototype

    override var itemRequests: Map<ItemPrototypeName, Int> = json.items.orEmpty()

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
    public var autoLaunch: Boolean = json.auto_launch

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        if (prototype.fixed_recipe.isNotEmpty())
            json.recipe = prototype.fixed_recipe
        json.auto_launch = autoLaunch
    }
}

public class Furnace(
    override val prototype: FurnacePrototype,
    json: EntityJson,
) : CraftingMachine(json)
