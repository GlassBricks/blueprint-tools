package glassbricks.factorio.blueprint.placement


class SlidingWindowMin<T : Any>(private val comparator: Comparator<T>) {
    private val deque = ArrayDeque<T>()
    fun add(value: T) {
        while (deque.isNotEmpty() && comparator.compare(deque.last(), value) > 0) deque.removeLast()
        deque.add(value)
    }

    fun min(): T? = deque.firstOrNull()
    fun removeMin() = deque.removeFirstOrNull()
    inline fun removeWhile(predicate: (T) -> Boolean) {
        while (true) {
            val first = min() ?: return
            if (!predicate(first)) return
            removeMin()
        }
    }
}
