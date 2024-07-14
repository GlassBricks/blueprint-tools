package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CableConnectionData
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.json.EntityNumber

public interface CableConnectionPoint {
    public val cableConnections: CableConnections
    public val entity: Entity
}

public enum class PowerSwitchConnectionSide { Left, Right }


public interface PowerSwitchConnectionPoint : CableConnectionPoint {
    public val side: PowerSwitchConnectionSide
    public override val entity: PowerSwitchConnections
}

public interface PowerSwitchConnections : Entity {
    public val left: PowerSwitchConnectionPoint
    public val right: PowerSwitchConnectionPoint
}

public fun PowerSwitchConnections.getCableConnectionPoint(side: PowerSwitchConnectionSide): PowerSwitchConnectionPoint =
    when (side) {
        PowerSwitchConnectionSide.Left -> left
        PowerSwitchConnectionSide.Right -> right
    }

public val PowerSwitchConnections.leftConnections: CableConnections get() = left.cableConnections
public val PowerSwitchConnections.rightConnections: CableConnections get() = right.cableConnections


/**
 * A point for cable connections.
 */
public sealed interface CableConnections : MutableSet<CableConnectionPoint> {
    public val parent: CableConnectionPoint
}

public fun CableConnections(parent: CableConnectionPoint): CableConnections = CableConnectionSetImpl(parent)

internal open class CableConnectionSetImpl(override val parent: CableConnectionPoint) :
    NotifyingSet<CableConnectionPoint>(),
    CableConnections {
    override fun onAdd(element: CableConnectionPoint): Boolean {
        if (element.entity == parent.entity) return false
        return (element.cableConnections as CableConnectionSetImpl).inner.add(parent)
    }

    override fun onRemove(element: CableConnectionPoint) {
        (element.cableConnections as CableConnectionSetImpl).inner.remove(parent)
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
    override fun toString(): String = "CableConnections(parent=$parent)"
}

internal fun CableConnectionPoint.exportNeighbors(
    entityMap: Map<Entity, EntityJson>,
): List<EntityNumber>? = cableConnections.mapNotNull { pt ->
    entityMap[pt.entity]?.entity_number
}.takeIf { it.isNotEmpty() }

internal fun CableConnectionPoint.exportPowerSwitch(
    entityMap: Map<Entity, EntityJson>,
): List<CableConnectionData>? = cableConnections.mapNotNull { pt ->
    CableConnectionData(entityMap[pt.entity]?.entity_number ?: return@mapNotNull null)
}.takeIf { it.isNotEmpty() }
