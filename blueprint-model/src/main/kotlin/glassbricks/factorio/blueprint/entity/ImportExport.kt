package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*


internal fun BlueprintPrototypes.createEntityFromJson(
    json: EntityJson,
    originalBlueprint: BlueprintJson? = null,
): Entity {
    val prototype = this.prototypes[json.name] ?: UnknownPrototype(json.name)
    return createEntityFromPrototype(prototype, jsonInit(json, originalBlueprint))
}


internal fun BlueprintPrototypes.entitiesFromJson(json: BlueprintJson): List<Entity>? {
    val jsonEntities = json.entities
    if (jsonEntities.isNullOrEmpty()) return null

    val indexByEntityNumber = jsonEntities.indices.associateBy { jsonEntities[it].entity_number }
    val entities = jsonEntities.map { createEntityFromJson(it, json) }

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
        connectAtColor(point.red, json.red)
        connectAtColor(point.green, json.green)
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
        // todo: power switches
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
            val p1 = entity.connectionPoint1.export(entityMap)
            val p2 = entity.connectionPoint2?.export(entityMap)
            if (p1 != null || p2 != null)
                json.connections = Connections(p1, p2)
        } 
        if (entity is CableConnectionPoint) {
            json.neighbours = entity.exportNeighbors(entityMap)
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
