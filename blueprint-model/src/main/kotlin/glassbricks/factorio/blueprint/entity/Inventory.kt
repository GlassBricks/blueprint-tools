package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.ItemFilter

public interface WithBar {
    public var bar: Int?
}

/**
 * Represents item filters.
 *
 * Used in cargo wagons, filter inserters, loaders, and modded filtered containers.
 */
public interface WithItemFilters {
    public val filters: Array<String?>
    public val numFilters: Int get() = filters.size
}

public class WithItemFiltersMixin(
    numFilters: Int,
    source: List<ItemFilter>? = null,
) : WithItemFilters {
    override val filters: Array<String?> = arrayOfNulls(numFilters)

    init {
        if (source != null) for (filter in source) {
            if (filter.index - 1 in filters.indices) {
                filters[filter.index - 1] = filter.name
            }
        }
    }

}

public fun WithItemFilters.getFiltersAsList(): List<ItemFilter> = filters.mapIndexedNotNull { index, name ->
    name?.let { ItemFilter(name = it, index = index + 1) }
}
