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
            prototype(name) {
                tryAddProperty("energy_source")
                tryAddProperty("fluid_box")
                tryAddProperty("output_fluid_box")
                block()
            }
        }

        // all blueprint-able prototypes...
        blueprintable("AccumulatorPrototype") {
            +"circuit_wire_max_distance"
            +"default_output_signal"
        }
        blueprintable("ArtilleryTurretPrototype")
        blueprintable("BeaconPrototype") {
            +"supply_area_distance"
            +"distribution_effectivity"
            +"module_specification"
            +"allowed_effects"
        }
        blueprintable("BoilerPrototype")
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

        "FluidID" {}
        "ProductionType" {}
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
