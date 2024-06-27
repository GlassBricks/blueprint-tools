package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.Inventory
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.json.ScheduleRecord
import glassbricks.factorio.prototypes.CargoWagonPrototype
import glassbricks.factorio.prototypes.LocomotivePrototype
import glassbricks.factorio.prototypes.RollingStockPrototype


public sealed interface RollingStock : Entity {
    override val prototype: RollingStockPrototype
    public var orientation: Double
}

public class CargoWagon
internal constructor(
    override val prototype: CargoWagonPrototype,
    init: EntityInit,
) : BaseEntity(init),
    RollingStock,
    WithBar,
    WithItemFilters {
    override var orientation: Double = init.json?.orientation ?: 0.0
    override var bar: Int? = init.json?.inventory?.bar
    override val filters: Array<String?> =
        init.json?.inventory?.filters.toFilters(prototype.inventory_size.toInt())

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        val filters = this.getFiltersAsList()
        if (filters != null || bar != null) {
            json.inventory = Inventory(filters = filters.orEmpty(), bar = bar)
        }
    }
}

public class Locomotive
internal constructor(
    override val prototype: LocomotivePrototype,
    init: EntityInit,
) : BaseEntity(init),
    RollingStock,
    WithColor,
    WithItemRequests {
    override var orientation: Double = init.json?.orientation ?: 0.0
    override var color: Color? = init.json?.color
    public override val itemRequests: MutableMap<ItemPrototypeName, Int> = init.json?.items.orEmpty().toMutableMap()
    public var schedule: List<ScheduleRecord> = init.getSchedule()

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        json.color = color
        json.items = itemRequests.takeIf { it.isNotEmpty() }
        // schedule handled by blueprint export
    }
}

public class OtherRollingStock internal constructor(
    override val prototype: RollingStockPrototype,
    init: EntityInit,
) : BaseEntity(init),
    RollingStock {
    override var orientation: Double = init.json?.orientation ?: 0.0

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
    }
}
