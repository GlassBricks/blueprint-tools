package glassbricks.factorio

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

        // all blueprint-able prototypes...
        "AccumulatorPrototype" {
            +"energy_source"
            +"circuit_wire_max_distance"
            +"default_output_signal"
        }
        "ArtilleryTurretPrototype" {}
        "BeaconPrototype" {
            +"energy_source"
            +"supply_area_distance"
            +"distribution_effectivity"
            +"module_specification"
            +"allowed_effects"
        }
    }

    concepts {
        "ItemID" {}
        "ItemToPlace" {}
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

        "BaseEnergySource" {
            includeAllProperties = false
        }
        "VoidEnergySource" {}
        "ElectricEnergySource" {
            includeAllProperties = false
            +"type"
        }
    }
}
