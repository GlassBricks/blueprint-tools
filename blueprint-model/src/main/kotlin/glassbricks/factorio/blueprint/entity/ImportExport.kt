package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype


public fun createEntityFromPrototype(prototype: EntityWithOwnerPrototype, source: EntityProps): Entity {
    return getConstructorForPrototype(prototype)(prototype, source)
}

internal fun BlueprintPrototypes.createEntityFromJson(
    json: EntityJson,
    originalBlueprint: BlueprintJson? = null,
): Entity {
    val prototype = this.prototypes[json.name] ?: UnknownPrototype(json.name)
    val source = FromJson(json, originalBlueprint)
    return createEntityFromPrototype(prototype, source)
}

internal fun BlueprintPrototypes.entitiesFromJson(json: BlueprintJson): List<Entity>? {
    val jsonEntities = json.entities
    if (jsonEntities.isNullOrEmpty()) return null

    val indexByEntityNumber = jsonEntities.indices.associateBy { jsonEntities[it].entity_number }
    val entities = jsonEntities.map { createEntityFromJson(it, json) }

    // connect circuit wires
    fun connectAtColor(
        set: ConnectionPoint.ConnectionSet,
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
        point: ConnectionPoint,
        json: ConnectionPointJson,
    ) {
        connectAtColor(point.red, json.red)
        connectAtColor(point.green, json.green)
    }

    for ((index, entity) in entities.withIndex()) {
        val jsonEntity = jsonEntities[index]
        val connections = jsonEntity.connections ?: continue
        if (entity !is CircuitConnectable) continue

        connections.`1`?.let { connectAtPoint(entity.connectionPoint1, it) }
        connections.`2`?.let { ptJson ->
            entity.connectionPoint2?.let { pt -> connectAtPoint(pt, ptJson) }
        }
    }
    
    return entities
}

public fun BlueprintJson.setEntitiesFrom(entities: Iterable<Entity>) {
    val schedules = mutableMapOf<List<ScheduleRecord>, MutableList<EntityNumber>>()
    val connectionPointMap = mutableMapOf<ConnectionPoint, EntityNumber>()
    val circuitConnectableEntities = mutableListOf<CircuitConnectable>()

    var nextId = 1
    val entityMap = entities.associateWith { entity ->
        val entityNumber = EntityNumber(nextId++)
        val json = entity.toJsonIsolated(entityNumber)
        require(entityNumber == entityNumber) {
            "Entity number changed after creation: $entityNumber -> ${json.entity_number}"
        }
        if (entity is WithSchedule) {
            val schedule = entity.schedule
            if (schedule.isNotEmpty())
                schedules.getOrPut(schedule, ::mutableListOf).add(entityNumber)
        }
        if (entity is CircuitConnectable) {
            circuitConnectableEntities.add(entity)
            connectionPointMap[entity.connectionPoint1] = entityNumber
            entity.connectionPoint2?.let {
                connectionPointMap[it] = entityNumber
            }
        }
        json
    }

    for (entity in circuitConnectableEntities) {
        val json = entityMap[entity as Entity]!!
        val p1 = entity.connectionPoint1.export(connectionPointMap)
        val p2 = entity.connectionPoint2?.export(connectionPointMap)
        if (p1 != null || p2 != null)
            json.connections = Connections(p1, p2)
    }

    val schedulesJson: List<Schedule>? = schedules.entries
        .takeIf { it.isNotEmpty() }
        ?.map { (schedule, locomotives) ->
            Schedule(schedule = schedule, locomotives = locomotives)
        }

    this.entities = entityMap.values.toList()
    this.schedules = schedulesJson
}

private typealias Constructor = (EntityWithOwnerPrototype, EntityProps) -> Entity

private inline fun <reified T : EntityWithOwnerPrototype>
        MutableMap<Class<out EntityWithOwnerPrototype>, Constructor>.matcher(
    noinline constructor: (T, EntityProps) -> Entity,
) {
    @Suppress("UNCHECKED_CAST")
    put(T::class.java, constructor as Constructor)
}


private val matcherMap = buildMap {
    matcher(::CargoWagon)
    matcher(::Locomotive)
    matcher(::OtherRollingStock)
    matcher(::UnknownEntity)
}

private val constructorCache = hashMapOf<Class<out EntityWithOwnerPrototype>, Constructor>()

private fun getConstructorForPrototype(
    prototype: EntityWithOwnerPrototype,
): Constructor = constructorCache.getOrPut(prototype::class.java) {
    var clazz: Class<*>? = prototype.javaClass
    while (clazz != null) {
        matcherMap[clazz]?.let {
            return@getOrPut it
        }
        clazz = clazz.superclass
    }
    throw AssertionError("All prototypes should be caught by UnknownEntity")
}
