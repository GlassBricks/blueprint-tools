package glassbricks.factorio.blueprint.placement

typealias MultiMap<K, V> = Map<K, Set<V>>

interface MutableMultiMap<K, V> : Map<K, MutableSet<V>> {
    fun add(key: K, value: V): Boolean
    fun addAll(key: K, values: Collection<V>): Boolean

    fun removeKey(key: K): Collection<V>?
    fun removeValue(key: K, value: V): Boolean
    fun clear()
}

private class HashMultiMap<K, V> : HashMap<K, MutableSet<V>>(), MutableMultiMap<K, V> {
    override fun add(key: K, value: V): Boolean = getOrPut(key) { hashSetOf() }.add(value)
    override fun addAll(key: K, values: Collection<V>): Boolean = getOrPut(key) { hashSetOf() }.addAll(values)

    override fun removeKey(key: K): Collection<V>? = remove(key)
    override fun removeValue(key: K, value: V): Boolean {
        val set = get(key) ?: return false
        if (set.remove(value)) {
            if (set.isEmpty()) remove(key)
            return true
        }
        return false
    }

    override fun clear() = super.clear()
}

fun <K, V> MultiMap(): MutableMultiMap<K, V> = HashMultiMap()

inline fun <K, V> MultiMap<K, V>.filterKeysM(predicate: (K) -> Boolean): MutableMultiMap<K, V> {
    val newMap = MultiMap<K, V>()
    for ((key, value) in this) {
        if (predicate(key)) newMap.addAll(key, value)
    }
    return newMap
}
