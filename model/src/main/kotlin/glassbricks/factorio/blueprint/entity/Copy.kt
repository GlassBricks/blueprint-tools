package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.SpatialDataStructure


private fun addOldCableConnections(
    old: CableConnectionPoint,
    new: CableConnectionPoint,
) {
    new.cableConnections.addAll(old.cableConnections)
}

private fun addOldCircuitConnections(
    old: CircuitConnectionPoint,
    new: CircuitConnectionPoint,
) {
    new.circuitConnections.red.addAll(old.circuitConnections.red)
    new.circuitConnections.green.addAll(old.circuitConnections.green)
}

/**
 * Creates a copy of this entity, also copying connections other entities.
 *
 * The old neighbors will have connections references to the resulting entity.
 *
 * If you want copy multiple entities with a copy of their connections, instead of connecting to old entities,
 * see [copyEntitiesWithConnections] instead.
 */
public fun <T : BlueprintEntity> T.copyWithOldConnections(): T {
    val old = this

    @Suppress("UNCHECKED_CAST")
    val new = copyIsolated() as T
    // cable connections
    if (old is CableConnectionPoint) {
        new as CableConnectionPoint
        addOldCableConnections(old, new)
    } else if (old is PowerSwitchConnections) {
        new as PowerSwitchConnections
        addOldCableConnections(old.left, new.left)
        addOldCableConnections(old.right, new.right)
    }

    // circuit connections
    if (old is CircuitConnectionPoint) {
        new as CircuitConnectionPoint
        addOldCircuitConnections(old, new)
    } else if (old is CombinatorConnections) {
        new as CombinatorConnections
        addOldCircuitConnections(old.input, new.input)
        addOldCircuitConnections(old.output, new.output)
    }

    return new
}

private fun copyCableConnections(
    old: CableConnectionPoint,
    new: CableConnectionPoint,
    resultMap: Map<out BlueprintEntity, BlueprintEntity>,
) {
    for (oldOtherPoint in old.cableConnections) {
        val newEntity = resultMap[oldOtherPoint.entity] ?: continue
        val newPoint = when (newEntity) {
            is CableConnectionPoint -> newEntity
            is PowerSwitchConnections -> newEntity.getCableConnectionPoint((oldOtherPoint as PowerSwitchConnectionPoint).side)
            else -> continue
        }
        new.cableConnections.add(newPoint)
    }
}

private fun copyCircuitConnections(
    old: CircuitConnectionSet,
    new: CircuitConnectionSet,
    resultMap: Map<out BlueprintEntity, BlueprintEntity>,
) {
    for (oldOtherPoint in old) {
        val newEntity = resultMap[oldOtherPoint.entity] ?: continue
        val newPoint = when (newEntity) {
            is CircuitConnectionPoint -> newEntity
            is CombinatorConnections -> newEntity.getCircuitConnectionPoint(oldOtherPoint.circuitID)
            else -> continue
        }
        new.add(newPoint)
    }
}

private fun copyCircuitConnections(
    old: CircuitConnectionPoint,
    new: CircuitConnectionPoint,
    resultMap: Map<out BlueprintEntity, BlueprintEntity>,
) {
    copyCircuitConnections(old.circuitConnections.red, new.circuitConnections.red, resultMap)
    copyCircuitConnections(old.circuitConnections.green, new.circuitConnections.green, resultMap)
}


/**
 * Copies all entities in this list, also copying connections between them.
 *
 * No connections to old entities will be added; for that, use [copyWithOldConnections] instead.
 *
 * Connections to entities not in the provided list will not be copied.
 *
 * Returns a map of old entities to new entities.
 */
public fun <T : BlueprintEntity> copyEntitiesWithConnections(entities: Iterable<T>): Map<T, T> {
    @Suppress("UNCHECKED_CAST")
    val resultMap = entities.associateWith { it.copyIsolated() as T }
    for ((old, new) in resultMap) {
        if (old is CableConnectionPoint) {
            copyCableConnections(old, new as CableConnectionPoint, resultMap)
        } else if (old is PowerSwitchConnections) {
            new as PowerSwitchConnections
            copyCableConnections(old.left, new.left, resultMap)
            copyCableConnections(old.right, new.right, resultMap)
        }

        if (old is CircuitConnectionPoint) {
            copyCircuitConnections(old, new as CircuitConnectionPoint, resultMap)
        } else if (old is CombinatorConnections) {
            new as CombinatorConnections
            copyCircuitConnections(old.input, new.input, resultMap)
            copyCircuitConnections(old.output, new.output, resultMap)
        }
    }

    return resultMap
}


/** Removes all cable and circuit connections from this entity. */
public fun BlueprintEntity.removeAllConnections() {
    if (this is CableConnectionPoint) {
        cableConnections.clear()
    } else if (this is PowerSwitchConnections) {
        left.cableConnections.clear()
        right.cableConnections.clear()
    }
    if (this is CircuitConnectionPoint) {
        circuitConnections.red.clear()
        circuitConnections.green.clear()
    } else if (this is CombinatorConnections) {
        inputConnections.clear()
        outputConnections.clear()
    }
}

public fun <T : BlueprintEntity> MutableCollection<T>.removeWithConnections(entity: T) {
    if (remove(entity)) {
        entity.removeAllConnections()
    }
}

public fun <T : BlueprintEntity> MutableCollection<T>.removeAllWithConnections(entities: Iterable<T>) {
    for (entity in entities) {
        removeWithConnections(entity)
    }
}

public inline fun <T : BlueprintEntity> MutableCollection<T>.removeWithConnectionsIf(crossinline predicate: (T) -> Boolean): Boolean =
    removeIf {
        predicate(it).also { removed ->
            if (removed) {
                it.removeAllConnections()
            }
        }
    }

public fun <T : BlueprintEntity> SpatialDataStructure<T>.copyEntities(): MutableSpatialDataStructure<T> {
    return DefaultSpatialDataStructure<T>()
        .also {
            it.addAll(copyEntitiesWithConnections(this).values)
        }
}
