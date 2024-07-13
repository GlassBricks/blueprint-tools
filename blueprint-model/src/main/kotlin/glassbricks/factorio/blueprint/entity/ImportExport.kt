package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes


public fun BlueprintPrototypes.entityFromJson(
    json: EntityJson,
    blueprint: BlueprintJson? = null,
): Entity {
    val prototype = this.blueprintableEntities[json.name] ?: UnknownPrototype(json.name)
    return createEntity(prototype, json, blueprint)
}

public fun BlueprintPrototypes.createEntity(
    name: String,
    position: Position,
    direction: Direction = Direction.North,
): Entity {
    val json = EntityJson(EntityNumber(1), name, position, direction)
    return entityFromJson(json)
}

private val CircuitConnectable.connectionPoint1: CircuitConnectionPoint
    get() = when (this) {
        is CombinatorConnections -> input
        is CircuitConnectionPoint -> this
    }

private val CircuitConnectable.connectionPoint2: CircuitConnectionPoint?
    get() = when (this) {
        is CombinatorConnections -> output
        else -> null
    }

private fun CircuitConnectable.getConnectionPoint(id: CircuitID): CircuitConnectionPoint? = when (this) {
    is CombinatorConnections -> this.getConnectionPoint(id)
    is CircuitConnectionPoint -> if (id == CircuitID.First) this else null
}


internal fun BlueprintPrototypes.entitiesFromJson(json: BlueprintJson): List<Entity>? {
    val jsonEntities = json.entities
    if (jsonEntities.isNullOrEmpty()) return null

    val indexByEntityNumber = jsonEntities.indices.associateBy { jsonEntities[it].entity_number }
    val entities = jsonEntities.map { entityFromJson(it, json) }

    // connect circuit wires and copper wires
    fun connectAtColor(
        set: CircuitConnectionSet,
        json: List<ConnectionData>?,
    ) {
        if (json.isNullOrEmpty()) return
        for (data in json) {
            val index = indexByEntityNumber[data.entity_id] ?: continue
            val entity = entities[index]
            if (entity !is CircuitConnectable) continue
            val connectionPoint = entity.getConnectionPoint(data.circuit_id) ?: continue
            set.add(connectionPoint)
        }
    }

    fun connectAtPoint(
        point: CircuitConnectionPoint,
        json: ConnectionPointJson,
    ) {
        connectAtColor(point.circuitConnections.red, json.red)
        connectAtColor(point.circuitConnections.green, json.green)
    }

    fun connectPowerSwitch(
        point: CableConnectionPoint,
        json: List<CableConnectionData>,
    ) {
        for (data in json) {
            val index = indexByEntityNumber[data.entity_id] ?: continue
            val entity = entities[index]
            if (entity !is ElectricPole) continue
            point.cableConnections.add(entity)
        }
    }

    for ((index, entity) in entities.withIndex()) {
        val jsonEntity = jsonEntities[index]
        if (entity is CircuitConnectable) jsonEntity.connections?.let { connections ->
            connections.`1`?.let {
                connectAtPoint(entity.connectionPoint1, it)
            }
            connections.`2`?.let { ptJson ->
                entity.connectionPoint2?.let { pt -> connectAtPoint(pt, ptJson) }
            }
        }
        if (entity is CableConnectionPoint) jsonEntity.neighbours?.let { neighbors ->
            for (neighbor in neighbors) {
                val otherIndex = indexByEntityNumber[neighbor] ?: continue
                val other = entities[otherIndex]
                if (other is CableConnectionPoint) {
                    entity.cableConnections.add(other)
                }
            }
        }
        if (entity is PowerSwitchConnectionPoints) jsonEntity.connections?.let { connections ->
            connections.Cu0?.let { connectPowerSwitch(entity.left, it) }
            connections.Cu1?.let { connectPowerSwitch(entity.right, it) }
        }
    }

    return entities
}

internal fun BlueprintJson.setEntitiesFrom(entities: Iterable<Entity>) {
    val schedules = mutableMapOf<List<ScheduleRecord>, MutableList<EntityNumber>>()

    var nextId = 1
    val entityMap = entities.associateWith { entity ->
        val entityNumber = EntityNumber(nextId++)
        val json = entity.toJsonIsolated(entityNumber)
        require(entityNumber == entityNumber) {
            "Entity number changed after creation: $entityNumber -> ${json.entity_number}"
        }
        json
    }

    for ((entity, json) in entityMap) {
        if (entity is Locomotive) {
            val schedule = entity.schedule
            if (schedule.isNotEmpty())
                schedules.getOrPut(schedule, ::mutableListOf).add(json.entity_number)
        }
        if (entity is CircuitConnectable) {
            val p1 = entity.connectionPoint1.circuitConnections.export(entityMap)
            val p2 = entity.connectionPoint2?.circuitConnections?.export(entityMap)
            if (p1 != null || p2 != null)
                json.connections = Connections(p1, p2)
        }
        if (entity is CableConnectionPoint) {
            json.neighbours = entity.exportNeighbors(entityMap)
        }
        if (entity is PowerSwitchConnectionPoints) {
            val cu0 = entity.left.exportPowerSwitch(entityMap)
            val cu1 = entity.right.exportPowerSwitch(entityMap)
            if (cu0 != null || cu1 != null)
                json.connections = json.connections?.copy(Cu0 = cu0, Cu1 = cu1)
                    ?: Connections(Cu0 = cu0, Cu1 = cu1)
        }
    }

    val schedulesJson: List<Schedule>? = schedules.entries
        .takeIf { it.isNotEmpty() }
        ?.map { (schedule, locomotives) ->
            Schedule(schedule = schedule, locomotives = locomotives)
        }

    this.entities = entityMap.values.toList()
    this.schedules = schedulesJson
}
