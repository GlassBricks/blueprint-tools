package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityFilterMode
import glassbricks.factorio.blueprint.json.InfinitySettings
import glassbricks.factorio.prototypes.ContainerPrototype
import glassbricks.factorio.prototypes.InfinityContainerPrototype
import glassbricks.factorio.prototypes.LogisticContainerPrototype
import glassbricks.factorio.blueprint.json.InfinityFilter as InfinityFilterJson
import glassbricks.factorio.blueprint.json.LogisticFilter as LogisticFilterJson

public open class Container
internal constructor(
    prototype_: ContainerPrototype,
    init: EntityInit,
) : BaseEntity(init),
    WithBar, WithItemFilters {
    override val prototype: ContainerPrototype = prototype_
    public override val filters: Array<String?> = init.json?.filters.toFilters(prototype_.inventory_size.toInt())
    public override var bar: Int? = init.json?.bar

    // containers have control behavior, but it has no settings (always read chest contents)

    override fun exportToJson(json: EntityJson) {
        json.bar = bar
        json.filters = getFiltersAsList()
    }
}

public open class LogisticContainer
internal constructor(
    prototype_: LogisticContainerPrototype,
    init: EntityInit,
) : Container(prototype_, init) {
    override val prototype: LogisticContainerPrototype get() = super.prototype as LogisticContainerPrototype
    public val requestFilters: Array<LogisticRequest?> = init.json?.request_filters.toLogiFilters(
        prototype_.max_logistic_slots?.toInt() ?: prototype_.inventory_size.toInt()
    )
    public val requestFromBuffers: Boolean = init.json?.request_from_buffers ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.request_filters = requestFilters.mapIndexedNotNull { index, request ->
            request?.let { LogisticFilterJson(name = it.item, count = it.count, index = index + 1) }
        }
        json.request_from_buffers = requestFromBuffers
    }
}

public data class LogisticRequest(
    val item: String,
    val count: Int,
)

public class InfinityContainer
internal constructor(
    prototype_: InfinityContainerPrototype,
    init: EntityInit,
) : LogisticContainer(prototype_, init) {
    override val prototype: InfinityContainerPrototype get() = super.prototype as InfinityContainerPrototype
    public val infinityFilters: Array<InfinityFilter?> = init.json?.infinity_settings?.filters.toInfinityFilters(prototype.inventory_size.toInt())
    public val removeUnfilteredItems: Boolean = init.json?.infinity_settings?.remove_unfiltered_items ?: false

    override fun exportToJson(json: EntityJson) {
        super.exportToJson(json)
        json.infinity_settings = InfinitySettings(
            remove_unfiltered_items = removeUnfilteredItems,
            filters = infinityFilters.mapIndexedNotNull { index, filter ->
                filter?.let { InfinityFilterJson(name = it.name, count = it.count, mode = it.mode, index = index + 1) }
            }
        )
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
                    item = filter.name,
                    count = filter.count
                )
            }
        }
    }

private fun List<InfinityFilterJson>?.toInfinityFilters(size: Int): Array<InfinityFilter?> =
    arrayOfNulls<InfinityFilter>(size).also { filters ->
        this?.forEach { filter ->
            if (filter.index - 1 in filters.indices) {
                filters[filter.index - 1] = InfinityFilter(
                    name = filter.name,
                    count = filter.count,
                    mode = filter.mode
                )
            }
        }
    }
