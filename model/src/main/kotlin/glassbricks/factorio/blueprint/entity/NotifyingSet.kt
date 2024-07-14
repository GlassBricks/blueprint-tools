package glassbricks.factorio.blueprint.entity

internal abstract class NotifyingSet<T : Any> : AbstractMutableSet<T>() {
    protected val inner: MutableSet<T> = LinkedHashSet()

    protected abstract fun onAdd(element: T): Boolean
    protected abstract fun onRemove(element: T)

    override fun iterator(): MutableIterator<T> = Iterator(inner.iterator())

    override val size: Int get() = inner.size
    override fun add(element: T): Boolean =
        onAdd(element) && inner.add(element)

    override fun remove(element: T): Boolean = inner.remove(element)
        .also { if (it) onRemove(element) }

    override fun contains(element: T): Boolean = inner.contains(element)
    override fun clear() {
        for (element in inner) onRemove(element)
        inner.clear()
    }

    private inner class Iterator(val iterator: MutableIterator<T>) : MutableIterator<T> {
        private var last: T? = null
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): T = iterator.next().also { last = it }
        override fun remove() {
            iterator.remove()
            onRemove(last!!)
        }
    }


}
