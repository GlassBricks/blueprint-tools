package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.ItemFilter
import glassbricks.factorio.blueprint.json.ItemPrototypeName

public interface WithColor {
    public var color: Color?
}

/**
 * If this entity can have item requests.
 *
 * Currently only includes: crafting machines, locomotives, and beacons.
 */
public interface WithItemRequests {
    public var itemRequests: Map<ItemPrototypeName, Int>
}

public interface WithModules : WithItemRequests

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

internal fun List<ItemFilter>?.toFilters(size: Int): Array<String?> =
    this.indexedToArray(size, ItemFilter::index, ItemFilter::name)

internal inline fun <T, reified R> List<T>?.indexedToArray(
    size: Int,
    getIndex: (T) -> Int,
    getValue: (T) -> R,
): Array<R?> = arrayOfNulls<R>(size).also { array ->
    this?.forEach { item ->
        val zeroIndex = getIndex(item) - 1
        if (zeroIndex in array.indices) {
            array[zeroIndex] = getValue(item)
        }
    }
}

internal inline fun <T, R> Array<out R?>.arrayToIndexedList(getValue: (Int, R) -> T?): List<T> =
    this.mapIndexedNotNull { index, item ->
        item?.let { getValue(index + 1, it) }
    }

public fun WithItemFilters.getFiltersAsList(): List<ItemFilter>? =
    filters.arrayToIndexedList { index, name -> ItemFilter(name = name, index = index) }
        .takeIf { it.isNotEmpty() }

internal fun itemFilterList(vararg origFilters: String?): List<ItemFilter>? =
    origFilters.arrayToIndexedList { index, name -> ItemFilter(name = name, index = index) }
        .takeIf { it.isNotEmpty() }
