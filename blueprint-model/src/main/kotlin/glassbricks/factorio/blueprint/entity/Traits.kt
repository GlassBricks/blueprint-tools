package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.ItemFilter
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.prototypes.EnergySource

public interface WithColor {
    public var color: Color?
}

internal val EntityInit<WithColor>.color: Color?
    get() = self?.color ?: json?.color

/**
 * If this entity can have item requests.
 *
 * Currently only includes: crafting machines, locomotives, and beacons.
 */
public interface WithItemRequests {
    public val itemRequests: MutableMap<ItemPrototypeName, Int>
}

public interface WithModules : WithItemRequests

internal val EntityInit<WithItemRequests>.itemRequests: MutableMap<ItemPrototypeName, Int>
    get() = self?.itemRequests?.toMutableMap() ?: json?.items?.toMutableMap() ?: mutableMapOf()

public interface WithBar {
    public var bar: Int?
}

public interface WithEnergySource {
    public val energySource: EnergySource
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
