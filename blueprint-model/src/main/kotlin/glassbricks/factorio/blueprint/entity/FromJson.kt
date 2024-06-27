package glassbricks.factorio.blueprint.entity


import glassbricks.factorio.blueprint.EntityNumber
import glassbricks.factorio.blueprint.Schedule
import glassbricks.factorio.blueprint.ScheduleRecord
import glassbricks.factorio.prototypes.EntityWithOwnerPrototype

public fun BlueprintJson.setEntitiesFrom(entities: Iterable<Entity>) {
    var nextId = 1
    val entityMap = entities.associateWith { entity ->
        val entityNumber = EntityNumber(nextId++)
        entity.toJsonIsolated(entityNumber)
            .also {
                require(it.entity_number == entityNumber) {
                    "Entity number changed after creation: ${it.entity_number} != $entityNumber"
                }
            }
    }

    val schedules = mutableMapOf<List<ScheduleRecord>, MutableList<EntityNumber>>()
    for ((entity, json) in entityMap) {
        entity.configureConnections(json, entityMap)

        if (entity is WithSchedule) {
            schedules.getOrPut(entity.schedule, ::mutableListOf).add(json.entity_number)
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

public fun createEntityFromPrototype(prototype: EntityWithOwnerPrototype, source: EntityProps): Entity {
    val constructor = getConstructorForPrototype(prototype)
    return constructor(prototype, source)
}

public fun BlueprintPrototypes.createEntityFromJson(
    json: EntityJson,
    originalBlueprint: BlueprintJson? = null,
): Entity {
    val prototype = this.prototypes[json.name] ?: UnknownPrototype(json.name)
    val source = FromJson(json, originalBlueprint)
    return createEntityFromPrototype(prototype, source)
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

private val constructorCache = mutableMapOf<Class<out EntityWithOwnerPrototype>, Constructor>()

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
