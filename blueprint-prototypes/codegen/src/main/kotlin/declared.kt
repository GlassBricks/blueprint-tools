package glassbricks.factorio

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName


fun GeneratedPrototypesBuilder.getGeneratedClasses() {
    extraSealedIntf("EVEnergySource", "ElectricEnergySource", "VoidEnergySource")
    extraSealedIntf("EHFVEnergySource", "ElectricEnergySource", "HeatEnergySource", "FluidEnergySource", "VoidEnergySource")

    prototypes {
        "PrototypeBase" {
            +"type"
            +"name"
        }
        "EntityPrototype" {
            +"collision_box"
            +"collision_mask"
            +"flags"
            +"build_grid_size"
            +"placeable_by"
            +"tile_width"
            +"tile_height"
        }
        "EntityWithHealthPrototype" {}
        "EntityWithOwnerPrototype" {}

        fun blueprintable(
            name: String,
            block: GeneratedPrototypeBuilder.() -> Unit = {}
        ) {
            prototype(name + "Prototype") {
                tryAddProperty("energy_source")
                tryAddProperty("fluid_box")
                tryAddProperty("output_fluid_box")
                tryAddProperty("circuit_wire_max_distance")
                tryAddProperty("allowed_effects")
                tryAddProperty("base_productivity")
                tryAddProperty("module_specification")
                block()
            }
        }

        // all blueprint-able prototypes...
        blueprintable("Accumulator") {
            +"default_output_signal"
        }
        blueprintable("ArtilleryTurret")
        blueprintable("Beacon") {
            +"supply_area_distance"
            +"distribution_effectivity"
        }
        blueprintable("Boiler")
        blueprintable("BurnerGenerator") {
            +"burner"
        }
        blueprintable("Combinator")
        blueprintable("ArithmeticCombinator")
        blueprintable("DeciderCombinator")
        blueprintable("ConstantCombinator") {
            +"item_slot_count"
        }
        blueprintable("Container") {
            +"inventory_size"
            "inventory_type" {
                innerEnumName = "InventoryType"
            }
        }
        blueprintable("LogisticContainer") {
            "logistic_mode" {
                innerEnumName = "LogisticMode"
                inner.optional = true
            }
            +"max_logistic_slots"

        }
        blueprintable("InfinityContainer")
        blueprintable("CraftingMachine") {
            +"crafting_speed"
            +"crafting_categories"
            +"fluid_boxes"
        }
        blueprintable("AssemblingMachine") {
            +"fixed_recipe"
            +"ingredient_count"
        }
        blueprintable("RocketSilo")
        blueprintable("Furnace")
        blueprintable("ElectricEnergyInterface")
        blueprintable("ElectricPole") {
            +"supply_area_distance"
            +"maximum_wire_distance"
        }
        blueprintable("Gate")
        blueprintable("Generator")
        blueprintable("HeatInterface")
        blueprintable("HeatPipe")
        blueprintable("Inserter") {
            +"insert_position"
            +"pickup_position"
            +"filter_count"
        }
        blueprintable("Lab") {
            +"inputs"
        }
        blueprintable("Lamp")
        blueprintable("LandMine")
        blueprintable("LinkedContainer") {
            +"inventory_size"
            "inventory_type" {
                innerEnumName = "InventoryType"
            }
        }
        blueprintable("MiningDrill") {
            +"vector_to_place_result"
            +"resource_categories"
        }
        blueprintable("OffshorePump") {
            +"fluid"
        }
        blueprintable("Pipe")
        blueprintable("InfinityPipe")
        blueprintable("PipeToGround")
        blueprintable("PowerSwitch")
        blueprintable("ProgrammableSpeaker") {
            +"maximum_polyphony"
            +"instruments"
        }
        blueprintable("Pump")
        blueprintable("Radar") {
            +"max_distance_of_sector_revealed"
            +"max_distance_of_nearby_sector_revealed"
        }
        blueprintable("Rail")
        blueprintable("StraightRail")
        blueprintable("RailSignalBase") {
            +"default_red_output_signal"
            +"default_orange_output_signal"
            +"default_green_output_signal"
        }
        blueprintable("RailChainSignal") {
            +"default_blue_output_signal"
        }
        blueprintable("RailSignal")
        blueprintable("Reactor")
        blueprintable("Roboport") {
            +"logistics_radius"
            +"construction_radius"
            +"default_available_logistic_output_signal"
            +"default_total_logistic_output_signal"
            +"default_available_construction_output_signal"
            +"default_total_construction_output_signal"
            +"logistics_connection_distance"
        }
        blueprintable("SimpleEntityWithOwner")
        blueprintable("SimpleEntityWithForce")
        blueprintable("SolarPanel")
        blueprintable("StorageTank") {
            +"two_direction_only"
        }
        blueprintable("TrainStop") {
            +"default_train_stopped_signal"
            +"default_trains_count_signal"
            +"default_trains_limit_signal"
        }
        blueprintable("TransportBeltConnectable") {
            +"speed"
        }
        blueprintable("LinkedBelt") {
            +"allow_clone_connection"
            +"allow_blueprint_connection"
            +"allow_side_loading"
        }
        blueprintable("Loader") {
            +"filter_count"
        }
        blueprintable("Loader1x1")
        blueprintable("Loader1x2")
        blueprintable("Splitter")
        blueprintable("TransportBelt") {
            +"related_underground_belt"
        }
        blueprintable("UndergroundBelt") {
            +"max_distance"
        }
        blueprintable("Turret")
        blueprintable("AmmoTurret")
        blueprintable("Wall")
    }

    concepts {
        "ItemID" {}
        "EntityID" {}
        "ItemToPlace" {}

        "Vector" {
            overrideType = ClassName(PAR_PACKAGE_NAME, "Position")
        }
        "MapPosition" {
            overrideType = ClassName(PAR_PACKAGE_NAME, "Position")
        }

        "CollisionMask"(fun GeneratedConceptBuilder.() {
            overrideType = List::class.parameterizedBy(String::class)
        })

        "EntityPrototypeFlags"(fun GeneratedConceptBuilder.() {
            innerEnumName = "EntityPrototypeFlag"
        })

        "SignalIDConnector"(fun GeneratedConceptBuilder.() {
            "name" {
                overrideType = String::class.asClassName()
            }
            "type" {
                innerEnumName = "SignalType"
            }
        })

        "ItemStackIndex" {}
        "ModuleSpecification" {
            includeAllProperties = false
            +"module_slots"
        }
        "EffectTypeLimitation" {
            innerEnumName = "EffectType"
        }

        "RecipeID" {}
        "RecipeCategoryID" {}
        "ResourceCategoryID" {}
        "ProductionType" {}

        "FluidID" {}
        "FluidBox" {
            includeAllProperties = false
            +"pipe_connections"
            +"filter"
            +"production_type"
        }
        "PipeConnectionDefinition" {
            "type" {
                innerEnumName = "InputOutputType"
            }
        }

        "FuelCategoryID" {}
        "HeatConnection" {}

        "BaseEnergySource" {
            includeAllProperties = false
        }
        "EnergySource" {
            overrideType = ClassName(PACKAGE_NAME, "BaseEnergySource")
        }
        "VoidEnergySource" {}
        "BurnerEnergySource" {
            includeAllProperties = false
            +"type"
            +"fuel_category"
            +"fuel_categories"
        }
        "HeatEnergySource" {
            includeAllProperties = false
            +"type"
            +"connections"
        }
        "FluidEnergySource" {
            includeAllProperties = false
            +"type"
            +"fluid_box"
        }
        "ElectricEnergySource" {
            includeAllProperties = false
            +"type"
        }

        "ProgrammableSpeakerInstrument" {}
        "ProgrammableSpeakerNote" {
            includeAllProperties = false
            +"name"
        }
    }
}
