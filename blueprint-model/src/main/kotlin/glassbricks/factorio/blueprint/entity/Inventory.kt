package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.ItemFilter

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

internal fun List<ItemFilter>?.toFilters(size: Int): Array<String?> = arrayOfNulls<String>(size).also { filters ->
    this?.forEach { filter ->
        if (filter.index - 1 in filters.indices) {
            filters[filter.index - 1] = filter.name
        }
    }
}

public fun WithItemFilters.getFiltersAsList(): List<ItemFilter>? = filters.mapIndexedNotNull { index, name ->
    name?.let { ItemFilter(name = it, index = index + 1) }
}.takeIf { it.isNotEmpty() }
