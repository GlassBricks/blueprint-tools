package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.CargoWagonPrototype
import glassbricks.factorio.blueprint.prototypes.ItemStackIndex
import glassbricks.factorio.blueprint.prototypes.LocomotivePrototype
import glassbricks.factorio.blueprint.prototypes.RollingStockPrototype


public sealed interface RollingStock : BlueprintEntity {
    override val prototype: RollingStockPrototype
    public var orientation: Double

    override fun copyIsolated(): RollingStock
}

internal fun getSchedule(
    json: EntityJson?,
    originalBlueprint: BlueprintJson?,
): List<ScheduleRecord> {
    if (json == null || originalBlueprint == null) return emptyList()
    return originalBlueprint.schedules
        ?.firstOrNull { it.locomotives.any { number -> number == json.entity_number } }
        ?.schedule
        .orEmpty()
}

public class CargoWagon(
    override val prototype: CargoWagonPrototype,
    json: EntityJson,
) : BaseEntity(json),
    RollingStock,
    WithInventory {
    override var orientation: Double = json.orientation ?: 0.0

    override var bar: Int? = json.inventory?.bar
    override val inventorySize: ItemStackIndex get() = prototype.inventory_size
    override val allowsFilters: Boolean get() = true
    override val filters: Array<String?> = json.inventory?.filters.toFilterArray(prototype.inventory_size.toInt())

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        val filters = this.filtersAsIndexList()
        if (filters != null || bar != null) {
            json.inventory = Inventory(filters = filters.orEmpty(), bar = bar)
        }
    }

    override fun copyIsolated(): CargoWagon = CargoWagon(prototype, jsonForCopy())
}

public class Locomotive(
    override val prototype: LocomotivePrototype,
    json: EntityJson,
    blueprint: BlueprintJson?,
) : BaseEntity(json),
    RollingStock,
    WithColor,
    WithItemRequests {
    override var orientation: Double = json.orientation ?: 0.0
    override var color: Color? = json.color
    public override var itemRequests: Map<ItemPrototypeName, Int> = json.items.orEmpty()
    public var schedule: List<ScheduleRecord> = getSchedule(json, blueprint)

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        json.color = color
        json.items = itemRequests.takeIf { it.isNotEmpty() }
        // schedule handled by blueprint export
    }

    override fun copyIsolated(): Locomotive =
        Locomotive(prototype, jsonForCopy(), null)
            .also { it.schedule = this.schedule }
}

public class OtherRollingStock(
    override val prototype: RollingStockPrototype,
    json: EntityJson,
) : BaseEntity(json),
    RollingStock {
    override var orientation: Double = json.orientation ?: 0.0

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
    }

    override fun copyIsolated(): OtherRollingStock = OtherRollingStock(prototype, jsonForCopy())
}
