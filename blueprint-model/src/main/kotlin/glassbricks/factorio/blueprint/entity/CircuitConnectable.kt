package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CircuitID
import glassbricks.factorio.blueprint.json.ConnectionData

public sealed interface CircuitConnectable

/**
 * A connection point on an entity.
 *
 * This can either be the entity itself, or for combinators, separate point1 and point2 objects.
 *
 * Can be connected to other [CircuitConnectionPoint]s, by accessing [circuitConnections].
 *
 * Adding or removing connections will also update the connected points' sets.
 *
 * This class's equals and hashCode are based on reference equality; so two connection points with the same connections are not equal.
 */
public interface CircuitConnectionPoint : CircuitConnectable {
    public val circuitConnections: CircuitConnections
    public val circuitID: CircuitID get() = CircuitID.First
    public val entity: Entity get() = this as Entity
}

public fun CircuitConnectionPoint.hasCircuitConnections(): Boolean = !circuitConnections.isEmpty()

/**
 * An entity with two connection points.
 *
 * This currently only includes decider and arithmetic combinators.
 */
public interface CombinatorConnections : CircuitConnectable {
    public val input: CircuitConnectionPoint
    public val output: CircuitConnectionPoint
}

public val CombinatorConnections.inputs: CircuitConnections get() = input.circuitConnections
public val CombinatorConnections.outputs: CircuitConnections get() = output.circuitConnections
public fun CombinatorConnections.getConnectionPoint(id: CircuitID): CircuitConnectionPoint = when (id) {
    CircuitID.First -> input
    CircuitID.Second -> output
}


public enum class WireColor { Red, Green }

public sealed interface CircuitConnectionSet : MutableSet<CircuitConnectionPoint> {
    public val color: WireColor
}

public class CircuitConnections(public val parent: CircuitConnectionPoint) {

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

    internal inner class ConnectionSetImpl(override val color: WireColor) : NotifyingSet<CircuitConnectionPoint>(),
        CircuitConnectionSet {
        override fun onAdd(element: CircuitConnectionPoint): Boolean {
            if (element == parent) return false
            return (element.circuitConnections[color] as ConnectionSetImpl).inner.add(parent)
        }

        override fun onRemove(element: CircuitConnectionPoint) {
            (element.circuitConnections[color] as ConnectionSetImpl).inner.remove(parent)
        }

        internal fun export(parentMap: Map<Entity, EntityJson>): List<ConnectionData>? =
            if (isEmpty()) null else mapNotNull {
                val other = parentMap[it.entity] ?: return@mapNotNull null
                ConnectionData(other.entity_number, it.circuitID)
            }.takeIf { it.isNotEmpty() }
    }

    internal fun export(parentMap: Map<Entity, EntityJson>): ConnectionPointJson? {
        val red = (red as ConnectionSetImpl).export(parentMap)
        val green = (green as ConnectionSetImpl).export(parentMap)
        return if (red != null || green != null) ConnectionPointJson(red, green) else null
    }
}

public interface WithControlBehavior {
    public val controlBehavior: ControlBehavior
}
