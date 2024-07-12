package glassbricks.factorio

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName


fun GeneratedPrototypesBuilder.getGeneratedClasses() {
    extraSealedIntf("EVEnergySource", "ElectricEnergySource", "VoidEnergySource")

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
            +"module_specification"
            +"allowed_effects"
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
            +"allowed_effects"
            +"base_productivity"
            +"module_specification"
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
    }

    concepts {
        "ItemID" {}
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
    }
}
