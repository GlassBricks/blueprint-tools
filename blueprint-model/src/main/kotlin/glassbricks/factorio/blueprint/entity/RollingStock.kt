package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Color
import glassbricks.factorio.blueprint.Inventory
import glassbricks.factorio.blueprint.ScheduleRecord
import glassbricks.factorio.prototypes.CargoWagonPrototype
import glassbricks.factorio.prototypes.LocomotivePrototype
import glassbricks.factorio.prototypes.RollingStockPrototype


public sealed interface RollingStock : Entity {
    public var orientation: Double
}

public class CargoWagon(
    override val prototype: CargoWagonPrototype,
    source: EntityProps,
) : BaseEntity(source),
    RollingStock,
    WithItemFilters by WithItemFiltersMixin(prototype.inventory_size.toInt(), source.asJson()?.inventory?.filters) {
    override var orientation: Double = source.asJson()?.orientation ?: 0.0
    public var bar: Int? = source.asJson()?.inventory?.bar

    override fun copy(): CargoWagon = CargoWagon(prototype, this).also {
        it.orientation = orientation
        it.bar = bar
    }

    override fun configure(json: EntityJson) {
        json.orientation = orientation

        val filters = this.getFiltersAsList()
        if (filters.isNotEmpty() || this.bar != null) {
            json.inventory = Inventory(
                filters = filters,
                bar = this.bar,
            )
        }
    }

}

public class Locomotive(
    override val prototype: LocomotivePrototype,
    source: EntityProps,
) : BaseEntity(source), RollingStock, WithSchedule {
    override var orientation: Double = source.asJson()?.orientation ?: 0.0
    public var color: Color? = source.asJson()?.color
    public override var schedule: List<ScheduleRecord> = source.asFromJson()?.getSchedule().orEmpty()

    override fun copy(): Locomotive = Locomotive(prototype, this).also {
        it.orientation = orientation
        it.color = color
        it.schedule = schedule
    }

    override fun configure(json: EntityJson) {
        json.orientation = orientation
        json.color = color
    }
}

public class OtherRollingStock(
    override val prototype: RollingStockPrototype,
    source: EntityProps,
) : BaseEntity(source), RollingStock {
    override var orientation: Double = source.asJson()?.orientation ?: 0.0

    override fun copy(): OtherRollingStock = OtherRollingStock(prototype, this).also {
        it.orientation = orientation
    }

    override fun configure(json: EntityJson) {
        json.orientation = orientation
    }

}
