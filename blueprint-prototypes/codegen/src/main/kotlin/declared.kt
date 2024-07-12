package glassbricks.factorio

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName


fun GeneratedPrototypesBuilder.getGeneratedClasses() {
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

        // all blueprintable prototypes...
        "AccumulatorPrototype" {
            +"energy_source"
            +"circuit_wire_max_distance"
            +"default_output_signal"
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
        })

        "BaseEnergySource" {
            includeAllProperties = false
        }
        "ElectricEnergySource" {
            includeAllProperties = false
            +"type"
        }
    }
}
