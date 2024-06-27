package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.Inventory
import glassbricks.factorio.blueprint.json.ScheduleRecord
import glassbricks.factorio.prototypes.CargoWagonPrototype
import glassbricks.factorio.prototypes.LocomotivePrototype
import glassbricks.factorio.prototypes.RollingStockPrototype


public sealed interface RollingStock : Entity {
    override val prototype: RollingStockPrototype
    public var orientation: Double
}

private val EntityInit<RollingStock>.orientation: Double
    get() = self?.orientation ?: json?.orientation ?: 0.0

public class CargoWagon
internal constructor(
    override val prototype: CargoWagonPrototype,
    init: EntityInit<CargoWagon>,
) : BaseEntity(init),
    RollingStock,
    WithBar,
    WithItemFilters {
    override var orientation: Double = init.orientation
    override var bar: Int? = init.self?.bar ?: init.json?.inventory?.bar
    override val filters: Array<String?> =
        init.self?.filters?.copyOf() ?: init.json?.inventory?.filters.toFilters(prototype.inventory_size.toInt())

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        val filters = this.getFiltersAsList()
        if (filters != null || bar != null) {
            json.inventory = Inventory(filters = filters.orEmpty(), bar = bar)
        }
    }

    override fun copy(): CargoWagon = CargoWagon(prototype, copyInit(this))
}

public typealias WithSchedule = Locomotive

public class Locomotive
internal constructor(
    override val prototype: LocomotivePrototype,
    init: EntityInit<Locomotive>,
) : BaseEntity(init),
    RollingStock,
    WithColor
{
    override var orientation: Double = init.orientation
    override var color: Color? = init.color
    public var schedule: List<ScheduleRecord> = init.self?.schedule ?: init.getSchedule()

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
        json.color = color
        // schedule handled by blueprint export
    }

    override fun copy(): Locomotive = Locomotive(prototype, copyInit(this))
}

public class OtherRollingStock internal constructor(
    override val prototype: RollingStockPrototype,
    init: EntityInit<OtherRollingStock>,
) : BaseEntity(init),
    RollingStock {
    override var orientation: Double = init.orientation

    override fun exportToJson(json: EntityJson) {
        json.orientation = orientation
    }

    override fun copy(): OtherRollingStock = OtherRollingStock(prototype, copyInit(this))
}
