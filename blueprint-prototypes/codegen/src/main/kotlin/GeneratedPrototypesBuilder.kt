package glassbricks.factorio

import com.squareup.kotlinpoet.TypeName

class SealedIntf(
    val name: String,
    val subtypes: Set<String>,
    val source: Concept?
)

class GeneratedPrototypes(
    val prototypes: Map<String, GeneratedPrototype>,
    val concepts: Map<String, GeneratedConcept>,
    val extraSealedIntfs: List<SealedIntf>
)

sealed interface GeneratedValue {
    val inner: ProtoOrConcept
    val includedProperties: Map<String, GeneratedProperty>
    val typeName: String?
}

class GeneratedPrototype(
    override val inner: Prototype,
    override val includedProperties: Map<String, GeneratedProperty>
) : GeneratedValue {
    override val typeName: String? = inner.typename
}

class GeneratedConcept(
    override val inner: Concept,
    val overrideType: TypeName?,
    val innerEnumName: String?,
    override val includedProperties: Map<String, GeneratedProperty>,
    override val typeName: String?
) : GeneratedValue

class GeneratedProperty(
    val inner: Property,
    val overrideType: TypeName?,
    val innerEnumName: String?
)

@DslMarker
annotation class GeneratedPrototypesDsl

@GeneratedPrototypesDsl
class GeneratedPrototypesBuilder(docs: PrototypeApiDocs) {
    private val origPrototypes = docs.prototypes.associateBy { it.name }
    private val origConcepts = docs.types.associateBy { it.name }
    private val prototypes = mutableMapOf<String, GeneratedPrototype>()
    private val concepts = mutableMapOf<String, GeneratedConcept>()

    private val extraSealedIntfs = mutableListOf<SealedIntf>()

    @GeneratedPrototypesDsl
    inner class Prototypes {
        fun prototype(
            name: String,
            block: GeneratedPrototypeBuilder.() -> Unit
        ) = this@GeneratedPrototypesBuilder.apply {
            val prototype = origPrototypes[name] ?: error("Prototype $name not found")
            prototypes[name] = GeneratedPrototypeBuilder(prototype).apply(block).build(prototype)
        }

        operator fun String.invoke(block: GeneratedPrototypeBuilder.() -> Unit) {
            prototype(this, block)
        }
    }

    inline fun prototypes(block: Prototypes.() -> Unit) = Prototypes().block()

    fun findConcept(name: String): Concept {
        return origConcepts[name] ?: error("Concept $name not found")
    }

    fun addConcept(name: String, concept: GeneratedConcept) {
        concepts[name] = concept
    }

    @GeneratedPrototypesDsl
    inner class Concepts {
        operator fun String.invoke(block: GeneratedConceptBuilder.() -> Unit) {
            this@GeneratedPrototypesBuilder.addConcept(
                this, GeneratedConceptBuilder(
                    this@GeneratedPrototypesBuilder.findConcept(this)
                ).apply(block).build()
            )
        }
    }

    inline fun concepts(block: Concepts.() -> Unit) = Concepts().block()

    fun extraSealedIntf(name: String, vararg values: String) {
        extraSealedIntfs.add(SealedIntf(name, values.toSet(), null))
    }

    fun definedSealedIntf(name: String) {
        val concept = findConcept(name)
        val type = concept.type
        check(type is UnionType) {
            "Not a sealed interface type"
        }
        val options = type.options
            .map {
                ((it.innerType() as? BasicType)?.value ?: error("Union option not a basic type"))
                    .also {
                        check(it in origConcepts) {
                            "Union option is not a concept"
                        }
                    }
            }
        extraSealedIntfs.add(SealedIntf(name, options.toSet(), concept))
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
        for (value in extraSealedIntfs.flatMap { it.subtypes }) {
            check(value in prototypes || value in concepts) {
                "Extra sealed interface value $value not found"
            }
        }
        return GeneratedPrototypes(prototypes, concepts, extraSealedIntfs)
    }
}

@GeneratedPrototypesDsl
class GeneratedPrototypeBuilder(val prototype: Prototype) {
    private val properties = mutableMapOf<String, GeneratedProperty>()

    fun tryAddProperty(
        name: String,
        block: PropertyOptionsBuilder.() -> Unit = {}
    ): Boolean {
        if (name in properties) error("Property $name already defined")
        val property = prototype.properties.find { it.name == name } ?: return false
        properties[name] = PropertyOptionsBuilder(property).apply(block).build()
        return true
    }

    fun property(
        name: String,
        block: PropertyOptionsBuilder.() -> Unit
    ) {
        if (!tryAddProperty(name, block)) error("Property $name not found")
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
    private val properties: MutableMap<String, GeneratedProperty> = mutableMapOf()

    var includeAllProperties: Boolean = true

    fun property(
        name: String,
        block: PropertyOptionsBuilder.() -> Unit
    ) {
        if (name in properties) error("Property $name already defined")
        val property = concept.properties?.find { it.name == name } ?: error("Property $name not found")
        properties[name] = PropertyOptionsBuilder(property).apply(block).build()
    }

    operator fun String.invoke(block: PropertyOptionsBuilder.() -> Unit) {
        property(this, block)
    }

    operator fun String.unaryPlus() {
        property(this) {}
    }

    private fun Property.getTypeValue(): String? {
        if (name != "type") return null
        return (type.innerType() as? LiteralType)?.value?.takeIf { it.isString }?.content
    }

    fun build(): GeneratedConcept {
        if (includeAllProperties)
            for (property in concept.properties.orEmpty()) {
                if (property.name !in properties) {
                    properties[property.name] = PropertyOptionsBuilder(property).build()
                }
            }
        val typeProperty = properties["type"]
        val typeName = typeProperty?.inner?.getTypeValue()
        if (typeName != null) {
            properties.remove("type")
        }

        return GeneratedConcept(
            concept,
            overrideType = overrideType,
            innerEnumName = innerEnumName,
            properties,
            typeName
        )
    }
}

@GeneratedPrototypesDsl
class PropertyOptionsBuilder(val property: Property) {
    var overrideType: TypeName? = null
    var innerEnumName: String? = null
    fun build() = GeneratedProperty(property, overrideType = overrideType, innerEnumName = innerEnumName)
}
