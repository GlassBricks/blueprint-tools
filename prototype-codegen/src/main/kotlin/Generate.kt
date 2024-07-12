package glassbricks.factorio

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

private const val PACKAGE_NAME = "glassbricks.factorio.blueprint.prototypes"

private val builtins = mapOf(
    "bool" to Boolean::class.asClassName(),
    "double" to Double::class.asClassName(),
    "float" to Float::class.asClassName(),
    "int8" to Byte::class.asClassName(),
    "int16" to Short::class.asClassName(),
    "int32" to Int::class.asClassName(),
    "string" to String::class.asClassName(),
    "uint8" to UByte::class.asClassName(),
    "uint16" to UShort::class.asClassName(),
    "uint32" to UInt::class.asClassName(),
    "uint64" to ULong::class.asClassName(),
)
val predefined = builtins + mapOf(
    "BoundingBox" to ClassName(PACKAGE_NAME, "BoundingBox"),
)

class PrototypeDeclarationsGenerator(private val input: GeneratedPrototypes) {
    private val file = FileSpec.builder(PACKAGE_NAME, "Prototypes")

    fun generate(): FileSpec {
        file.addKotlinDefaultImports()

        for (prototype in input.prototypes.values.sortedBy { it.inner.order }) {
            val clazz = generatePrototype(prototype)
            file.addType(clazz)
        }
        for (concept in input.concepts.values.sortedBy { it.inner.order }) {
            val clazz = generateConcept(concept)
            if (clazz is TypeAliasSpec) {
                file.addTypeAlias(clazz)
            } else if (clazz is TypeSpec) {
                file.addType(clazz)
            }
        }
        return file.build()
    }

    private fun Documentable.Builder<*>.addDescription(description: String?) {
        if (!description.isNullOrBlank()) {
            addKdoc(description)
        }
    }

    private fun generatePrototype(prototype: GeneratedPrototype): TypeSpec {
        return TypeSpec.interfaceBuilder(prototype.inner.name).apply {
            addDescription(prototype.inner.description)

            if (prototype.inner.parent != null) {
                check(prototype.inner.parent in input.prototypes) {
                    "Parent prototype not found: ${prototype.inner.parent}"
                }
                addSuperinterface(ClassName(PACKAGE_NAME, prototype.inner.parent))
            }

            for (property in prototype.includedProperties.values.sortedBy { it.property.order }) {
                addProperty(generateProperty(prototype, property))
            }
        }.build()
    }

    private fun generateStructConcept(
        concept: GeneratedConcept,
        name: String,
    ): TypeSpec {
        return TypeSpec.interfaceBuilder(name).apply {
            addDescription(concept.inner.description)
            for (property in concept.inner.properties!!) {
                addProperty(generateProperty(concept, GeneratedProperty(property)))
            }
        }.build()
    }

    private fun generateConcept(concept: GeneratedConcept): Any {
        val type = if (concept.overrideType != null) {
            GeneratedType(concept.overrideType, null)
        } else {
            mapTypeDefinition(concept.inner.type, concept, null, true)
        }
        return if (type.declaration != null) {
            type.declaration.toBuilder().apply {
                addDescription(concept.inner.description)
            }.build()
        } else {
            TypeAliasSpec.builder(concept.inner.name, type.putType()).apply {
                addDescription(concept.inner.description)
            }.build()
        }
    }


    private fun tryGetEnumOptions(type: TypeDefinition): List<LiteralType>? =
        if (type is UnionType && type.options.all { it is LiteralType && it.value.isString }) {
            @Suppress("UNCHECKED_CAST")
            type.options as List<LiteralType>
        } else {
            null
        }

    private fun generateEnumType(name: String, options: List<LiteralType>): TypeSpec =
        TypeSpec.enumBuilder(name).apply {
            for (option in options) {
                addEnumConstant(
                    option.value.content,
                    TypeSpec.anonymousClassBuilder().apply {
                        addDescription(option.description)
                    }.build()
                )
            }
        }.build()


    private fun tryGetItemOrArrayValue(
        type: UnionType
    ): TypeDefinition? {
        if (type.options.size != 2) return null
        val (first, second) = type.options
        if (second !is ArrayType || first != second.value) return null
        return first
    }

    private fun generateProperty(
        context: GeneratedValue,
        genProperty: GeneratedProperty
    ): PropertySpec {
        val property = genProperty.property
        val nullable = property.optional || property.default != null

        val type = mapTypeDefinition(property.type, context, genProperty, true)
            .putType()
            .copy(nullable = nullable)

        return PropertySpec.builder(property.name, type).apply {
            if (property.description.isNotBlank()) {
                addKdoc(property.description)
            }
        }.build()
    }

    private inner class GeneratedType(
        private val typeName: TypeName,
        val declaration: TypeSpec?
    ) {
        fun putType(): TypeName {
            if (declaration != null) {
                file.addType(declaration)
            }
            return typeName
        }
    }

    private fun TypeName.toGenType() = GeneratedType(this, null)
    private fun mapTypeDefinition(
        type: TypeDefinition,
        context: GeneratedValue,
        property: GeneratedProperty?,
        isRoot: Boolean = false
    ): GeneratedType = when (type) {
        is BasicType -> {
            val value = type.value
            if (value in predefined) {
                predefined[value]!!.toGenType()
            } else {
                check(value in input.concepts) {
                    "Type not in generated concepts: $value"
                }
                ClassName(PACKAGE_NAME, value).toGenType()
            }
        }

        is ArrayType -> List::class.asClassName()
            .parameterizedBy(mapTypeDefinition(type.value, context, property).putType())
            .toGenType()

        is DictType -> Map::class.asClassName()
            .parameterizedBy(
                mapTypeDefinition(type.key, context, property).putType(),
                mapTypeDefinition(type.value, context, property).putType(),
            )
            .toGenType()

        is LiteralType -> {
            val value = type.value
            when {
                value.isString -> ClassName(PACKAGE_NAME, "UnknownStringLiteral").toGenType()
                else -> error("Unhandled literal type: $value")
            }
        }

        is TypeType -> mapTypeDefinition(type.value, context, property, isRoot)
        StructType -> {
            check(context is GeneratedConcept)
            val name = if (isRoot) context.inner.name else context.inner.name + "Values"
            GeneratedType(ClassName(PACKAGE_NAME, name), generateStructConcept(context, name))
        }

        is TupleType -> error("TupleType not supported")
        is UnionType -> tryGetEnumOptions(type)?.let { options ->
            val name = if (context is GeneratedConcept) {
                if (isRoot && property == null) {
                    context.inner.name
                } else if (!isRoot && property == null) {
                    context.inner.name + "Value"
                } else {
                    error("todo")
                }
            } else {
                error("todo")
            }
            val enumType = generateEnumType(name, options)
            GeneratedType(ClassName(PACKAGE_NAME, name), enumType)
        } ?: tryGetItemOrArrayValue(type)?.let { item ->
            ClassName(PACKAGE_NAME, "ItemOrArray")
                .parameterizedBy(mapTypeDefinition(item, context, property).putType())
                .toGenType()
        } ?: error("UnionType not supported")
    }
}
