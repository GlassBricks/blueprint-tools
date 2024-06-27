package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.EntityNumber
import glassbricks.factorio.prototypes.ElectricPolePrototype

public interface CableConnectionPoint {
    public val cableConnections: CableConnectionSet
}

public interface PowerSwitch {
    public val powerSwitchLeft: CableConnectionPoint
    public val powerSwitchRight: CableConnectionPoint
}

/**
 * A point for cable connections.
 */
public sealed interface CableConnectionSet : MutableSet<CableConnectionPoint> {
    public val parent: CableConnectionPoint
}

public fun CableConnectionSet(parent: CableConnectionPoint): CableConnectionSet = CableConnectionSetImpl(parent)

private class CableConnectionSetImpl(override val parent: CableConnectionPoint) : UpdatingSet<CableConnectionPoint>(),
    CableConnectionSet {
    override fun onAdd(element: CableConnectionPoint) {
        (element.cableConnections as CableConnectionSetImpl).inner.add(parent)
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
    init: EntityInit<ElectricPole>,
) : BaseEntity(init), CableConnectionPoint, CircuitConnectable {
    override val cableConnections: CableConnectionSet = CableConnectionSet(this)
    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)

    override fun exportToJson(json: EntityJson) {
        // all connections handled by ImportExport
    }

    override fun copy(): ElectricPole = ElectricPole(prototype, copyInit(this))


}
internal fun CableConnectionPoint.exportNeighbors(
    entityMap: Map<Entity, EntityJson>,
): List<EntityNumber>? = cableConnections.mapNotNull { pt ->
    if (pt is Entity) {
        entityMap[pt]?.entity_number
    } else {
        null
    }
}.takeIf { it.isNotEmpty() }
