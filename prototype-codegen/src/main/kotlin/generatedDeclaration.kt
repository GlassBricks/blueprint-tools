package glassbricks.factorio

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy


fun GeneratedPrototypesBuilder.getGeneratedClasses() {
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

    concept("ItemID")
    concept("ItemToPlace")
    concept("CollisionMask") {
        overrideType = List::class.parameterizedBy(String::class)
    }
    concept("EntityPrototypeFlags")
}
