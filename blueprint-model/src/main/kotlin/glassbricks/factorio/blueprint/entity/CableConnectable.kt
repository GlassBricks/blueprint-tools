package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CableConnectionData
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.prototypes.ElectricPolePrototype
import glassbricks.factorio.prototypes.PowerSwitchPrototype

public interface CableConnectionPoint {
    public val cableConnections: CableConnectionSet
    public val entity: Entity
}

public interface PowerSwitchConnectionPoints {
    public val left: CableConnectionPoint
    public val right: CableConnectionPoint
}

public val PowerSwitchConnectionPoints.leftConnections: CableConnectionSet get() = left.cableConnections
public val PowerSwitchConnectionPoints.rightConnections: CableConnectionSet get() = right.cableConnections


/**
 * A point for cable connections.
 */
public sealed interface CableConnectionSet : MutableSet<CableConnectionPoint> {
    public val parent: CableConnectionPoint
}

public fun CableConnectionSet(parent: CableConnectionPoint): CableConnectionSet = CableConnectionSetImpl(parent)

private class CableConnectionSetImpl(override val parent: CableConnectionPoint) : UpdatingSet<CableConnectionPoint>(),
    CableConnectionSet {
    override fun onAdd(element: CableConnectionPoint): Boolean {
        if (element.entity == parent.entity) return false
        return (element.cableConnections as CableConnectionSetImpl).inner.add(parent)
    }

    override fun onRemove(element: CableConnectionPoint) {
        (element.cableConnections as CableConnectionSetImpl).inner.remove(parent)
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
    override fun toString(): String = "CableConnectionSet(parent=$parent)"
}

public class ElectricPole
internal constructor(
    override val prototype: ElectricPolePrototype,
    init: EntityInit,
) : BaseEntity(init), CableConnectionPoint, CircuitConnectable {
    override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)

    override val entity: Entity get() = this

    override fun exportToJson(json: EntityJson) {
        // all connections handled by ImportExport
    }
}

public class PowerSwitch
internal constructor(
    override val prototype: PowerSwitchPrototype,
    init: EntityInit,
) : BaseEntity(init), CircuitConnectable, PowerSwitchConnectionPoints {
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public override val left: CableConnectionPoint = ConnectionPoint()
    public override val right: CableConnectionPoint = ConnectionPoint()

    public var switchState: Boolean = init.json?.switch_state ?: false

    override fun exportToJson(json: EntityJson) {
        json.switch_state = switchState
    }

    private inner class ConnectionPoint : CableConnectionPoint {
        override val entity: PowerSwitch get() = this@PowerSwitch
        override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    }
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
