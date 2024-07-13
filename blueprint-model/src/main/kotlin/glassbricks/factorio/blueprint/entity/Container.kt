package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityFilterMode
import glassbricks.factorio.blueprint.json.InfinitySettings
import glassbricks.factorio.blueprint.json.LogisticContainerModeOfOperation
import glassbricks.factorio.blueprint.json.asMode
import glassbricks.factorio.blueprint.prototypes.*
import glassbricks.factorio.blueprint.json.InfinityFilter as InfinityFilterJson
import glassbricks.factorio.blueprint.json.LogisticFilter as LogisticFilterJson

@Suppress("LocalVariableName")
public open class Container(
    prototype_: ContainerPrototype,
    json: EntityJson,
) : BaseEntity(json), WithInventory, WithItemFilters {
    override val prototype: ContainerPrototype = prototype_

    public override var bar: Int? = json.bar
    override val inventorySize: ItemStackIndex get() = prototype.inventory_size
    override val allowsFilters: Boolean get() = prototype.inventory_type == InventoryType.with_filters_and_bar
    public override val filters: Array<String?> = json.filters.toFilterArray(prototype_.inventory_size.toInt())

    // ordinary containers have control behavior, but it has no settings (always read chest contents)

    override fun exportToJson(json: EntityJson) {
        json.bar = bar
        json.filters = filtersAsIndexList()
    }
}

public open class LogisticContainer(
    prototype_: LogisticContainerPrototype,
    json: EntityJson,
) : Container(prototype_, json) {
    override val prototype: LogisticContainerPrototype get() = super.prototype as LogisticContainerPrototype
    public val requestFilters: Array<LogisticRequest?> = json.request_filters.toFilterArray(
        prototype_.max_logistic_slots?.toInt() ?: 0
    )
    public val requestFromBuffers: Boolean = json.request_from_buffers

    public val controlBehavior: LogisticContainerControlBehavior =
        LogisticContainerControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.request_filters = requestFilters.toFilterList()
        json.request_from_buffers = requestFromBuffers
        json.control_behavior = controlBehavior.exportToJson()
    }
}

public class LogisticContainerControlBehavior(
    json: ControlBehaviorJson?,
) : ControlBehavior {
    public var modeOfOperation: LogisticContainerModeOfOperation = json?.circuit_mode_of_operation
        ?.asLogisticContainer() ?: LogisticContainerModeOfOperation.SendContents

    override fun exportToJson(): ControlBehaviorJson? {
        if (modeOfOperation == LogisticContainerModeOfOperation.SendContents) return null
        return ControlBehaviorJson(
            circuit_mode_of_operation = modeOfOperation.asMode(),
        )
    }
}

public data class LogisticRequest(
    val item: String,
    val count: Int,
)

public class InfinityContainer(prototype_: InfinityContainerPrototype, json: EntityJson) :
    LogisticContainer(prototype_, json) {
    override val prototype: InfinityContainerPrototype get() = super.prototype as InfinityContainerPrototype
    public val infinityFilters: Array<InfinityFilter?> =
        json.infinity_settings?.filters.toInfFilterArray(prototype.inventory_size.toInt())
    public val removeUnfilteredItems: Boolean = json.infinity_settings?.remove_unfiltered_items ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        val filters = infinityFilters.toInfFilterList()

        json.infinity_settings = InfinitySettings(
            remove_unfiltered_items = removeUnfilteredItems,
            filters = filters
        )
    }
}

public data class InfinityFilter(
    val name: String,
    val count: Int,
    val mode: InfinityFilterMode,
)

private fun List<LogisticFilterJson>?.toFilterArray(size: Int): Array<LogisticRequest?> =
    indexListToArray(size, LogisticFilterJson::index) { LogisticRequest(it.name, it.count) }

private fun Array<LogisticRequest?>.toFilterList(): List<LogisticFilterJson> =
    arrayToIndexList { index, request -> LogisticFilterJson(name = request.item, count = request.count, index = index) }

private fun List<InfinityFilterJson>?.toInfFilterArray(size: Int): Array<InfinityFilter?> =
    indexListToArray(size, InfinityFilterJson::index) { InfinityFilter(it.name, it.count, it.mode) }

private fun Array<InfinityFilter?>.toInfFilterList(): List<InfinityFilterJson> =
    arrayToIndexList { index, filter ->
        InfinityFilterJson(
            name = filter.name,
            count = filter.count,
            mode = filter.mode,
            index = index
        )
    }
