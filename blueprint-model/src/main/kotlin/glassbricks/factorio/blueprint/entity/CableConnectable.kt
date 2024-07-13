package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.CableConnectionData
import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.PowerSwitchPrototype

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

public class ElectricPole(
    override val prototype: ElectricPolePrototype,
    json: EntityJson,
) : BaseEntity(json), CableConnectionPoint, CircuitConnectionPoint {
    override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    override val circuitConnections: CircuitConnections = CircuitConnections(this)

    override val entity: Entity get() = this

    override fun exportToJson(json: EntityJson) {
        // all connections handled by ImportExport
    }
}

public class PowerSwitch(
    override val prototype: PowerSwitchPrototype,
    json: EntityJson,
) : BaseEntity(json), PowerSwitchConnectionPoints, CircuitConnectionPoint, WithControlBehavior {
    public override val left: CableConnectionPoint = ConnectionPoint()
    public override val right: CableConnectionPoint = ConnectionPoint()
    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    override val controlBehavior: PowerSwitchControlBehavior = PowerSwitchControlBehavior(json.control_behavior)

    public var switchState: Boolean = json.switch_state ?: true

    override fun exportToJson(json: EntityJson) {
        json.switch_state = switchState
        if(this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }

    private inner class ConnectionPoint : CableConnectionPoint {
        override val entity: PowerSwitch get() = this@PowerSwitch
        override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    }
}

public class PowerSwitchControlBehavior(
    source: ControlBehaviorJson?,
) : ControlBehavior {
    public var circuitCondition: CircuitCondition = source?.circuit_condition ?: CircuitCondition.DEFAULT

    override fun exportToJson(): ControlBehaviorJson = ControlBehaviorJson(
        circuit_condition = circuitCondition
    )
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
