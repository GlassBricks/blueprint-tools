package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.prototypes.EntityWithOwnerPrototype

internal fun createEntityFromPrototype(prototype: EntityWithOwnerPrototype, source: EntityInit<Nothing>): Entity {
    return getConstructorForPrototype(prototype)(prototype, source)
}

private typealias Constructor = (EntityWithOwnerPrototype, EntityInit<*>) -> Entity

private inline fun <reified T : EntityWithOwnerPrototype>
        MutableMap<Class<out EntityWithOwnerPrototype>, Constructor>.add(
    noinline constructor: (T, EntityInit<Nothing>) -> Entity,
) {
    @Suppress("UNCHECKED_CAST")
    put(T::class.java, constructor as Constructor)
}

private val matcherMap = buildMap {
    add(::CargoWagon)
    add(::Locomotive)
    add(::OtherRollingStock)
    add(::ElectricPole)
    add(::PowerSwitch)
    add(::AssemblingMachine)
    add(::Furnace)
    add(::RocketSilo)
    add(::TransportBelt)
    add(::UndergroundBelt)
    add(::Splitter)
    add(::Loader)
    add(::UnknownEntity)
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
