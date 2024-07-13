package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.ItemFilter
import glassbricks.factorio.blueprint.json.ItemPrototypeName
import glassbricks.factorio.blueprint.prototypes.ItemStackIndex

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

public fun WithItemFilters.filtersAsIndexList(): List<ItemFilter>? =
    filters.arrayToIndexList { index, name -> ItemFilter(name = name, index = index) }
        .takeIf { it.isNotEmpty() }


/**
 * An entity with a main inventory; i.e. containers and cargo wagons.
 */
public interface WithInventory : WithItemFilters {
    public var bar: Int?
    public val inventorySize: ItemStackIndex
    public val allowsFilters: Boolean
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

internal fun List<ItemFilter>?.toFilterArray(size: Int): Array<String?> =
    indexListToArray(size, ItemFilter::index, ItemFilter::name)

internal inline fun <T, reified R> List<T>?.indexListToArray(
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

internal inline fun <T, R> Array<out R?>.arrayToIndexList(getValue: (Int, R) -> T?): List<T> =
    this.mapIndexedNotNull { index, item ->
        item?.let { getValue(index + 1, it) }
    }

internal fun itemFilterList(vararg origFilters: String?): List<ItemFilter>? =
    origFilters.arrayToIndexList { index, name -> ItemFilter(name = name, index = index) }
        .takeIf { it.isNotEmpty() }
