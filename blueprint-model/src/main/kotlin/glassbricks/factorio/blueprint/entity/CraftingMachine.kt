package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.prototypes.*


public sealed class CraftingMachine(init: EntityInit<CraftingMachine>) : BaseEntity(init), WithModules, WithEnergySource {
    abstract override val prototype: CraftingMachinePrototype
    override val energySource: EnergySource get() = prototype.energy_source
    
    override val itemRequests: MutableMap<ItemPrototypeName, Int> = init.itemRequests
    
    override fun exportToJson(json: EntityJson) {
        json.items = itemRequests.takeIf { it.isNotEmpty() }
    }

    abstract override fun copy(): CraftingMachine
}


public open class AssemblingMachine
internal constructor(
    override val prototype: AssemblingMachinePrototype,
    init: EntityInit<AssemblingMachine>,
) : CraftingMachine(init) {
    public var recipe: String? = init.self?.recipe ?: init.json?.recipe

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.recipe = recipe
    }

    override fun copy(): AssemblingMachine = AssemblingMachine(prototype, copyInit(this))
}

public class RocketSilo
internal constructor(
    override val prototype: RocketSiloPrototype,
    init: EntityInit<RocketSilo>,
) : AssemblingMachine(prototype, init) {
    public var autoLaunch: Boolean = init.self?.autoLaunch ?: init.json?.auto_launch ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.auto_launch = autoLaunch.takeIf { it }
    }

    override fun copy(): RocketSilo = RocketSilo(prototype, copyInit(this))
}

public class Furnace
internal constructor(
    override val prototype: FurnacePrototype,
    init: EntityInit<Furnace>,
) : CraftingMachine(init) {

    override fun copy(): Furnace = Furnace(prototype, copyInit(this))
}
