package glassbricks.factorio

import com.squareup.kotlinpoet.TypeName


class GeneratedPrototypes(
    val prototypes: Map<String, GeneratedPrototype>,
    val concepts: Map<String, GeneratedConcept>
)

sealed interface GeneratedValue {
    val inner: ProtoOrConcept
}

class GeneratedPrototype(
    override val inner: Prototype,
    val includedProperties: Map<String, GeneratedProperty>
): GeneratedValue

class GeneratedConcept(
    override val inner: Concept,
    val overrideType: TypeName?,
    val innerEnumName: String?
): GeneratedValue

class GeneratedProperty(val property: Property)

@DslMarker
annotation class GeneratedPrototypesDsl


@GeneratedPrototypesDsl
class GeneratedPrototypesBuilder(private val docs: PrototypeApiDocs) {
    private val origPrototypes = docs.prototypes.associateBy { it.name }
    private val prototypes = mutableMapOf<String, GeneratedPrototype>()
    private val concepts = mutableMapOf<String, GeneratedConcept>()

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

    fun concept(
        name: String,
        block: GeneratedConceptBuilder.() -> Unit = {}
    ) {
        val concept = docs.types.find { it.name == name } ?: error("Concept $name not found")
        concepts[name] = GeneratedConceptBuilder(concept).apply(block).build()
    }

    fun build(): GeneratedPrototypes {
        for (genPrototype in prototypes.values) {
            val prototype = genPrototype.inner
            if (prototype.parent != null) {
                check(prototype.parent in prototypes) {
                    "Parent of ${prototype.name} (${prototype.parent}) not defined"
                }
            }
        }
        return GeneratedPrototypes(prototypes, concepts)
    }
}

@GeneratedPrototypesDsl
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


    fun build(prototype: Prototype): GeneratedPrototype = GeneratedPrototype(prototype, properties)
}

@GeneratedPrototypesDsl
class GeneratedConceptBuilder(private val concept: Concept) {
    var overrideType: TypeName? = null
    var innerEnumName: String? = null

    fun build(): GeneratedConcept {
        return GeneratedConcept(
            concept,
            overrideType = overrideType,
            innerEnumName = innerEnumName
        )
    }
}

@GeneratedPrototypesDsl
class PropertyOptionsBuilder(val property: Property) {
    // more stuff here maybe later
    fun build() = GeneratedProperty(property)
}
