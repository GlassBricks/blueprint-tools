package glassbricks.factorio


class GeneratedPrototypes(
    val prototypes: List<GeneratedPrototype>
)

class GeneratedPrototype(
    val prototype: Prototype,
    val includedProperties: Map<String, GeneratedProperty>
)

class GeneratedProperty(val property: Property)

@DslMarker
annotation class PrototypePropertiesDsl


@PrototypePropertiesDsl
class GeneratedPrototypesBuilder(docs: PrototypeApiDocs) {
    private val origPrototypes = docs.prototypes.associateBy { it.name }
    private val prototypes = mutableMapOf<String, GeneratedPrototype>()

    fun prototype(
        name: String,
        block: GeneratedPrototypeBuilder.() -> Unit
    ) {
        val prototype = origPrototypes[name] ?: error("Prototype $name not found")
        prototypes[name] = GeneratedPrototypeBuilder(prototype).apply(block).build(prototype)
    }

    operator fun String.invoke(block: GeneratedPrototypeBuilder.() -> Unit) {
        prototype(this, block)
    }

    fun build(): GeneratedPrototypes {
        for (genPrototype in prototypes.values) {
            val prototype = genPrototype.prototype
            if (prototype.parent != null) {
                check(prototype.parent in prototypes) {
                    "Parent of ${prototype.name} (${prototype.parent}) not defined"
                }
            }
        }
        return GeneratedPrototypes(prototypes.values.toList())
    }
}

@PrototypePropertiesDsl
class GeneratedPrototypeBuilder(val prototype: Prototype) {
    private val properties = mutableMapOf<String, GeneratedProperty>()

    fun property(
        name: String,
        block: PropertyOptionsBuilder.() -> Unit
    ) {
        if (name in properties) error("Property $name already defined")
        val property = prototype.properties.find { it.name == name } ?: error("Property $name not found")
        properties[name] = PropertyOptionsBuilder(property).apply(block).build()
    }

    operator fun String.invoke(block: PropertyOptionsBuilder.() -> Unit) {
        property(this, block)
    }

    operator fun String.unaryPlus() {
        property(this) {}
    }


    fun build(prototype: Prototype): GeneratedPrototype {
        check(properties.isNotEmpty())
        return GeneratedPrototype(prototype, properties)
    }
}

@PrototypePropertiesDsl
class PropertyOptionsBuilder(val property: Property) {
    // more stuff here maybe later
    fun build() = GeneratedProperty(property)
}
