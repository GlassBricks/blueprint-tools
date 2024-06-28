package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityFilterMode
import glassbricks.factorio.blueprint.json.InfinitySettings
import glassbricks.factorio.blueprint.json.LogisticContainerModeOfOperation
import glassbricks.factorio.blueprint.json.asMode
import glassbricks.factorio.prototypes.ContainerPrototype
import glassbricks.factorio.prototypes.InfinityContainerPrototype
import glassbricks.factorio.prototypes.LogisticContainerPrototype
import glassbricks.factorio.blueprint.json.InfinityFilter as InfinityFilterJson
import glassbricks.factorio.blueprint.json.LogisticFilter as LogisticFilterJson

public open class Container(
    prototype_: ContainerPrototype,
    json: EntityJson,
) : BaseEntity(json), WithBar, WithItemFilters {
    override val prototype: ContainerPrototype = prototype_
    public override val filters: Array<String?> = json.filters.toFilters(prototype_.inventory_size.toInt())
    public override var bar: Int? = json.bar

    // containers have control behavior, but it has no settings (always read chest contents)

    override fun exportToJson(json: EntityJson) {
        json.bar = bar
        json.filters = getFiltersAsList()
    }
}

public open class LogisticContainer(
    prototype_: LogisticContainerPrototype,
    json: EntityJson,
) : Container(prototype_, json) {
    override val prototype: LogisticContainerPrototype get() = super.prototype as LogisticContainerPrototype
    public val requestFilters: Array<LogisticRequest?> = json.request_filters.toLogiFilters(
        prototype_.max_logistic_slots?.toInt() ?: prototype_.inventory_size.toInt()
    )
    public val requestFromBuffers: Boolean = json.request_from_buffers

    public val controlBehavior: LogisticContainerControlBehavior =
        LogisticContainerControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.request_filters = requestFilters.mapIndexedNotNull { index, request ->
            request?.let { LogisticFilterJson(name = it.item, count = it.count, index = index + 1) }
        }
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
        json.infinity_settings?.filters.toInfinityFilters(prototype.inventory_size.toInt())
    public val removeUnfilteredItems: Boolean = json.infinity_settings?.remove_unfiltered_items ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.infinity_settings = InfinitySettings(remove_unfiltered_items = removeUnfilteredItems,
            filters = infinityFilters.mapIndexedNotNull { index, filter ->
                filter?.let { InfinityFilterJson(name = it.name, count = it.count, mode = it.mode, index = index + 1) }
            })
    }
}

public data class InfinityFilter(
    val name: String,
    val count: Int,
    val mode: InfinityFilterMode,
)

private fun List<LogisticFilterJson>?.toLogiFilters(size: Int): Array<LogisticRequest?> =
    arrayOfNulls<LogisticRequest>(size).also { filters ->
        this?.forEach { filter ->
            if (filter.index - 1 in filters.indices) {
                filters[filter.index - 1] = LogisticRequest(
                    item = filter.name, count = filter.count
                )
            }
        }
    }

private fun List<InfinityFilterJson>?.toInfinityFilters(size: Int): Array<InfinityFilter?> =
    arrayOfNulls<InfinityFilter>(size).also { filters ->
        this?.forEach { filter ->
            if (filter.index - 1 in filters.indices) {
                filters[filter.index - 1] = InfinityFilter(
                    name = filter.name, count = filter.count, mode = filter.mode
                )
            }
        }
    }
