package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitID
import glassbricks.factorio.blueprint.json.CircuitID.First
import glassbricks.factorio.blueprint.json.CircuitID.Second
import glassbricks.factorio.blueprint.json.ConnectionData

/**
 * An entity that can be connected with circuit wires.
 * 
 * Has 1 or 2 [CircuitConnectionPoint]s.
 */
public interface CircuitConnectable : Entity {
    public val connectionPoint1: CircuitConnectionPoint

    public val connectionPoint2: CircuitConnectionPoint? get() = null
}

/**
 * A [CircuitConnectable] with a second connection point.
 *
 * This currently only includes decider and arithmetic combinators.
 */
public interface CircuitConnectable2 : CircuitConnectable {
    public override val connectionPoint2: CircuitConnectionPoint
}


public fun CircuitConnectable.getConnectionPoint(circuitID: CircuitID): CircuitConnectionPoint? = when (circuitID) {
    First -> connectionPoint1
    Second -> connectionPoint2
}

public fun CircuitConnectable2.getConnectionPoint(circuitID: CircuitID): CircuitConnectionPoint = when (circuitID) {
    First -> connectionPoint1
    Second -> connectionPoint2
}

public fun CircuitConnectable.isEmpty(): Boolean = connectionPoint1.isEmpty() && connectionPoint2?.isEmpty() ?: true

public enum class WireColor { Red, Green }

public sealed interface CircuitConnectionSet : MutableSet<CircuitConnectionPoint> {
    public val color: WireColor
}

/**
 * A connection point on an entity.
 *
 * Can be connected to other [CircuitConnectionPoint]s.
 *
 * Provides [red] and [green] sets, from where you can connect to other connection points.
 * Adding or removing connections will also update the connected points' sets.
 *
 * This class's equals and hashCode are based on reference equality; so two connection points with the same connections are not equal.
 */
public class CircuitConnectionPoint(
    public val parent: CircuitConnectable,
    public val circuitID: CircuitID = First,
) {

    public val red: CircuitConnectionSet = ConnectionSetImpl(WireColor.Red)
    public val green: CircuitConnectionSet = ConnectionSetImpl(WireColor.Green)

    public operator fun get(color: WireColor): CircuitConnectionSet = when (color) {
        WireColor.Red -> red
        WireColor.Green -> green
    }

    public fun isEmpty(): Boolean = red.isEmpty() && green.isEmpty()
    public fun clear() {
        red.clear()
        green.clear()
    }

    internal inner class ConnectionSetImpl(override val color: WireColor) : UpdatingSet<CircuitConnectionPoint>(),
        CircuitConnectionSet {
        override fun onAdd(element: CircuitConnectionPoint) {
            (element[color] as ConnectionSetImpl).inner.add(this@CircuitConnectionPoint)
        }

        override fun onRemove(element: CircuitConnectionPoint) {
            (element[color] as ConnectionSetImpl).inner.remove(this@CircuitConnectionPoint)
        }

        fun export(parentMap: Map<Entity, EntityJson>): List<ConnectionData>? =
            if (isEmpty()) null else mapNotNull {
                val other = parentMap[it.parent] ?: return@mapNotNull null
                ConnectionData(other.entity_number, it.circuitID)
            }.takeIf { it.isNotEmpty() }
    }

    internal fun export(parentMap: Map<Entity, EntityJson>): ConnectionPointJson? {
        val red = (red as ConnectionSetImpl).export(parentMap)
        val green = (green as ConnectionSetImpl).export(parentMap)
        return if (red != null || green != null) ConnectionPointJson(red, green) else null
    }
}
