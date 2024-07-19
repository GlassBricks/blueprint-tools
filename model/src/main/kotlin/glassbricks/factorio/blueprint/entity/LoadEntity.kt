package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.EntityJson
import glassbricks.factorio.blueprint.prototypes.*

public fun createEntity(
    prototype: EntityPrototype,
    source: EntityJson,
    blueprint: BlueprintJson? = null,
): Entity {
    require(source.name == prototype.name) { "Mismatched entity name: ${source.name} != ${prototype.name}" }
    return getConstructorForPrototype(prototype)(prototype, source, blueprint)
}

private typealias Constructor = (EntityPrototype, EntityJson, BlueprintJson?) -> Entity

private inline fun <reified T : EntityPrototype>
        MutableMap<Class<out EntityPrototype>, Constructor>.add(
    crossinline constructor: (T, EntityJson, BlueprintJson?) -> Entity,
) {
    put(T::class.java) { prototype, source, blueprint ->
        constructor(prototype as T, source, blueprint)
    }
}

private inline fun <reified T : EntityPrototype>
        MutableMap<Class<out EntityPrototype>, Constructor>.add(
    crossinline constructor: (T, EntityJson) -> Entity,
) {
    put(T::class.java) { prototype, source, _ ->
        constructor(prototype as T, source)
    }
}

private val basicConstructor: Constructor = { prototype, source, _ ->
    BasicEntity(prototype, source)
}

private inline fun <reified T : EntityPrototype>
        MutableMap<Class<out EntityPrototype>, Constructor>.basic() {
    put(T::class.java, basicConstructor)
}

private val matcherMap = hashMapOf<Class<out EntityPrototype>, Constructor>().apply {
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
    add(::LinkedBelt)
    add(::Container)
    add(::LogisticContainer)
    add(::InfinityContainer)
    add(::LinkedContainer)
    add(::Inserter)
    add(::ProgrammableSpeaker)
    add(::TrainStop)
    add(::Roboport)
    add(::Lamp)
    add(::StorageTank)
    add(::ArithmeticCombinator)
    add(::DeciderCombinator)
    add(::ConstantCombinator)
    add(::Accumulator)
    add(::RailSignal)
    add(::RailChainSignal)
    add(::Wall)
    add(::MiningDrill)
    add(::Beacon)
    add(::InfinityPipe)
    add(::HeatInterface)
    add(::SimpleEntityWithOwner)
    add(::ElectricEnergyInterface)
    add(::Pump)
    add(::Lab)

    basic<LandMinePrototype>()
    basic<SolarPanelPrototype>()
    basic<GeneratorPrototype>()
    basic<PipePrototype>()
    basic<PlayerPortPrototype>()
    basic<BurnerGeneratorPrototype>()
    basic<TurretPrototype>()
    basic<BoilerPrototype>()
    basic<ArtilleryTurretPrototype>()
    basic<OffshorePumpPrototype>()
    basic<PipeToGroundPrototype>()
    basic<RailPrototype>()
    basic<HeatPipePrototype>()
    basic<RadarPrototype>()
    basic<GatePrototype>()
    basic<ReactorPrototype>()

    add(::UnknownEntity)
}

private fun getConstructorForPrototype(prototype: EntityPrototype): Constructor =
    getConstructorForClass(prototype.javaClass)

private fun getConstructorForClass(clazz: Class<out EntityPrototype>): Constructor =
    matcherMap.getOrPut(clazz) {
        @Suppress("UNCHECKED_CAST")
        val superclass = clazz.superclass as? Class<out EntityPrototype>
            ?: throw AssertionError("All prototypes should be caught by UnknownEntity")

        getConstructorForClass(superclass)
    }
