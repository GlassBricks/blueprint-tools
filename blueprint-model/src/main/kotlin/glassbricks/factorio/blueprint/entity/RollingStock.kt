package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.Inventory
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.json.ScheduleRecord
import glassbricks.factorio.blueprint.prototypes.CargoWagonPrototype
import glassbricks.factorio.blueprint.prototypes.LocomotivePrototype
import glassbricks.factorio.blueprint.prototypes.RollingStockPrototype


public sealed interface RollingStock : Entity {
    override val prototype: RollingStockPrototype
    public var orientation: Double
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
    WithBar,
    WithItemFilters {
    override var orientation: Double = json.orientation ?: 0.0
    override var bar: Int? = json.inventory?.bar
    override val filters: Array<String?> =
        json.inventory?.filters.toFilters(prototype.inventory_size.toInt())

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        val filters = this.getFiltersAsList()
        if (filters != null || bar != null) {
            json.inventory = Inventory(filters = filters.orEmpty(), bar = bar)
        }
    }
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
}
