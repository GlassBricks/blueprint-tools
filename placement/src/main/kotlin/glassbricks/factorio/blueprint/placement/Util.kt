package glassbricks.factorio.blueprint.placement

import java.util.EnumMap


internal inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
    val iter = iterator()
    return Array(size) {
        check(iter.hasNext())
        transform(iter.next())
    }
}

internal inline fun <K, V, R : Any> Map<K, V>.mapValuesNotNull(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val result = mutableMapOf<K, R>()
    for (entry in entries) {
        val newV = transform(entry)
        if (newV != null) result[entry.key] = newV
    }
    return result
}

inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> = EnumMap(K::class.java)
