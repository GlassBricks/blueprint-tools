package glassbricks.factorio.blueprint.placement


typealias MultiMap<K, V> = Map<K, List<V>>
typealias MutableMultiMap<K, V> = MutableMap<K, MutableList<V>>

fun <K, V> multiMapOf(): MultiMap<K, V> = emptyMap()
fun <K, V> multiMapOf(vararg pairs: Pair<K, V>): MultiMap<K, V> = pairs.groupBy({ it.first }, { it.second })
fun <K, V> mutableMultiMapOf(): MutableMultiMap<K, V> = mutableMapOf()
fun <K, V> mutableMultiMapOf(vararg pairs: Pair<K, V>): MutableMultiMap<K, V> =
    pairs.groupByTo(mutableMultiMapOf(), { it.first }, { it.second })

fun <K, V> MutableMultiMap<K, V>.add(key: K, value: V): Boolean = getOrPut(key) { mutableListOf() }.run {
    if (!contains(value)) add(value)
    else false
}

fun <K, V> MutableMultiMap<K, V>.addAll(key: K, values: Iterable<V>) {
    getOrPut(key) { mutableListOf() }.apply {
        for (value in values) {
            if (!contains(value)) add(value)
        }
    }
}

fun <K, V> MutableMultiMap<K, V>.removeKey(key: K): List<V>? = remove(key)
fun <K, V> MutableMultiMap<K, V>.removeValue(key: K, value: V): Boolean {
    val list = get(key) ?: return false
    if (list.remove(value)) {
        if (list.isEmpty()) remove(key)
        return true
    }
    return false
}

typealias Table<R, C, V> = Map<Pair<R, C>, V>
typealias MutableTable<R, C, V> = MutableMap<Pair<R, C>, V>

fun <R, C, V> tableOf(): Table<R, C, V> = emptyMap()

fun <R, C, V> tableOf(vararg pairs: Pair<Pair<R, C>, V>): Table<R, C, V> = pairs.toMap()

fun <R, C, V> mutableTableOf(): MutableTable<R, C, V> = mutableMapOf()
fun <R, C, V> mutableTableOf(vararg pairs: Pair<Pair<R, C>, V>): MutableTable<R, C, V> = pairs.toMap(mutableTableOf())

operator fun <R, C, V> MutableTable<R, C, V>.set(row: R, col: C, value: V) {
    this[row to col] = value
}

operator fun <R, C, V> Table<R, C, V>.get(row: R, col: C): V? {
    return this[row to col]
}

fun <R, C, V> MutableTable<R, C, V>.remove(row: R, col: C): V? = remove(row to col)

inline fun <R, C, V> MutableTable<R, C, V>.removeAll(predicate: (R, C, V) -> Boolean) {
    val iter = iterator()
    while (iter.hasNext()) {
        val (key, value) = iter.next()
        if (predicate(key.first, key.second, value)) {
            iter.remove()
        }
    }
}

inline fun <R, C, V> MutableTable<R, C, V>.retainAll(predicate: (R, C, V) -> Boolean) {
    removeAll { r, c, v -> !predicate(r, c, v) }
}


typealias MultiTable<R, C, V> = Table<R, C, List<V>>
typealias MutableMultiTable<R, C, V> = MutableTable<R, C, MutableList<V>>

fun <R, C, V> multiTableOf(): MultiTable<R, C, V> = tableOf()

fun <R, C, V> multiTableOf(vararg pairs: Pair<Pair<R, C>, V>): MultiTable<R, C, V> =
    pairs.groupBy({ it.first }, { it.second })

fun <R, C, V> mutableMultiTableOf(): MutableMultiTable<R, C, V> = mutableTableOf()

fun <R, C, V> mutableMultiTableOf(vararg pairs: Pair<Pair<R, C>, V>): MutableMultiTable<R, C, V> =
    pairs.groupByTo(mutableMultiTableOf(), { it.first }, { it.second })

fun <R, C, V> MutableMultiTable<R, C, V>.add(row: R, col: C, value: V) {
    val list = getOrPut(row to col) { mutableListOf() }
    list.add(value)
}

fun <R, C, V> MutableMultiTable<R, C, V>.remove(row: R, col: C, value: V): Boolean {
    val key = row to col
    val list = get(key) ?: return false
    if (!list.remove(value)) return false
    if (list.isEmpty()) remove(key)
    return true
}


inline fun <R, C, V> MutableMultiTable<R, C, V>.removeAllM(crossinline predicate: (R, C, V) -> Boolean) {
    val iter = iterator()
    while (iter.hasNext()) {
        val (key, value) = iter.next()
        value.removeAll { predicate(key.first, key.second, it) }
        if (value.isEmpty()) iter.remove()
    }
}

inline fun <R, C, V> MutableMultiTable<R, C, V>.retainAllM(crossinline predicate: (R, C, V) -> Boolean) {
    removeAllM { r, c, v -> !predicate(r, c, v) }
}
