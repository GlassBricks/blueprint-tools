package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitID
import glassbricks.factorio.blueprint.json.CircuitID.First
import glassbricks.factorio.blueprint.json.CircuitID.Second
import glassbricks.factorio.blueprint.json.ConnectionData
import glassbricks.factorio.blueprint.json.EntityNumber

/**
 * An entity that can be connected with circuit wires.
 *
 * This may or may not have a second connection point; see [CircuitConnectable2].
 */
public interface CircuitConnectable {
    public val connectionPoint1: ConnectionPoint
    
    public val connectionPoint2: ConnectionPoint?
        get() = (this as? CircuitConnectable2)?.connectionPoint2
}

/**
 * A [CircuitConnectable] guaranteed to have a second connection point.
 * 
 * This currently only includes decider and arithmetic combinators.
 */
public interface CircuitConnectable2 : CircuitConnectable {
    public override val connectionPoint2: ConnectionPoint
}


public fun CircuitConnectable.getConnectionPoint(circuitID: CircuitID): ConnectionPoint? = when (circuitID) {
    First -> connectionPoint1
    Second -> connectionPoint2
}

public fun CircuitConnectable2.getConnectionPoint(circuitID: CircuitID): ConnectionPoint = when (circuitID) {
    First -> connectionPoint1
    Second -> connectionPoint2
}

public fun CircuitConnectable.isEmpty(): Boolean = connectionPoint1.isEmpty() && connectionPoint2?.isEmpty() ?: true

// import/export handled separately in ImportExport.kt
internal class CircuitConnectable1Mixin : CircuitConnectable {
    override val connectionPoint1: ConnectionPoint = ConnectionPoint(First)
}

internal class CircuitConnectable2Mixin : CircuitConnectable2 {
    override val connectionPoint1: ConnectionPoint = ConnectionPoint(First)
    override val connectionPoint2: ConnectionPoint = ConnectionPoint(Second)
}


public enum class WireColor { Red, Green }

/**
 * Represents a connection point on a circuit entity.
 */
public class ConnectionPoint(public val circuitID: CircuitID) {

    public val red: ConnectionSet = ConnectionSet(WireColor.Red)
    public val green: ConnectionSet = ConnectionSet(WireColor.Green)

    public operator fun get(color: WireColor): ConnectionPoint.ConnectionSet = when (color) {
        WireColor.Red -> red
        WireColor.Green -> green
    }

    public fun isEmpty(): Boolean = red.isEmpty() && green.isEmpty()
    public fun clear() {
        red.clear()
        green.clear()
    }

    /**
     * A set of other points this point is connected to.
     *
     * Modifying this set will also update the connected points' sets as well.
     */
    public inner class ConnectionSet
    internal constructor(public val color: WireColor) : AbstractMutableSet<ConnectionPoint>() {
        private val inner = mutableSetOf<ConnectionPoint>()
        override fun iterator(): MutableIterator<ConnectionPoint> = Iterator(inner.iterator())
        private inner class Iterator(val iterator: MutableIterator<ConnectionPoint>) :
            MutableIterator<ConnectionPoint> {
            private var last: ConnectionPoint? = null
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): ConnectionPoint = iterator.next().also { last = it }
            override fun remove() {
                iterator.remove()
                last!![color].inner.remove(last)
            }
        }

        override val size: Int get() = inner.size
        override fun add(element: ConnectionPoint): Boolean = inner.add(element)
            .also { if (it) element[color].inner.add(this@ConnectionPoint) }

        override fun remove(element: ConnectionPoint): Boolean = inner.remove(element)
            .also { if (it) element[color].inner.remove(this@ConnectionPoint) }

        override fun contains(element: ConnectionPoint): Boolean = inner.contains(element)
        override fun clear() {
            for (element in inner) element[color].inner.remove(this@ConnectionPoint)
            inner.clear()
        }

        internal fun export(parentMap: Map<ConnectionPoint, EntityNumber>): List<ConnectionData>? = if (isEmpty()) null else mapNotNull {
            val entityNumber = parentMap[it] ?: return@mapNotNull null
            ConnectionData(entityNumber, it.circuitID)
        }.takeIf { it.isNotEmpty() }
    }

    internal fun export(parentMap: Map<ConnectionPoint, EntityNumber>): ConnectionPointJson? {
        val red = red.export(parentMap)
        val green = green.export(parentMap)
        return if (red != null || green != null) ConnectionPointJson(red, green) else null
    }
}
