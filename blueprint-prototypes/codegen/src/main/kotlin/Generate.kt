package glassbricks.factorio

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

private const val PACKAGE_NAME = "glassbricks.factorio.blueprint.prototypes"
private const val PAR_PACKAGE_NAME = "glassbricks.factorio.blueprint"

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
    "BoundingBox" to ClassName(PAR_PACKAGE_NAME, "BoundingBox"),
)

class PrototypeDeclarationsGenerator(private val input: GeneratedPrototypes) {
    private val file = FileSpec.builder(PACKAGE_NAME, "Prototypes")

    private val hasInheritors = mutableSetOf<String>()
    private val extraSealedSupertypes =
        input.extraSealedIntfs
            .flatMap { it.key.map { subType -> subType to it.value } }
            .groupBy({ it.first }, { it.second })

    fun generate(): FileSpec {
        findInheritors()

        setupFile()
        generatePrototypes()
        generateConcepts()
        generateExtraSealedIntfs()
        generateDataRaw()

        return file.build()
    }


    private fun setupFile() = file.apply {
        addFileComment("Generated by prototype-codegen")
        addAnnotation(
            AnnotationSpec.builder(Suppress::class.asClassName())
                .addMember("%S", "EnumEntryName")
                .addMember("%S", "PropertyName")
                .build()
        )
        addAnnotation(
            AnnotationSpec.builder(UseSerializers::class)
                .addMember("PositionShorthandSerializer::class")
                .addMember("BoundingBoxShorthandSerializer::class")
                .build()
        )
        addKotlinDefaultImports()
    }

    private fun findInheritors() {
        for (prototype in input.prototypes.values + input.concepts.values) {
            val parent = prototype.inner.parent
            if (parent != null) {
                hasInheritors.add(parent)
            }
        }
    }


    private fun generatePrototypes(): MutableSet<String> {
        val alreadyGenerated = mutableSetOf<String>()
        fun visitPrototype(prototype: GeneratedPrototype) {
            if (prototype.inner.name in alreadyGenerated) return
            alreadyGenerated.add(prototype.inner.name)

            val parent = prototype.inner.parent
            if (parent != null) {
                val parentPrototype = input.prototypes[parent] ?: error("Parent prototype not found: $parent")
                visitPrototype(parentPrototype)
            }
            file.addType(generatePrototype(prototype))
        }

        for (prototype in input.prototypes.values.sortedBy { it.inner.order }) {
            visitPrototype(prototype)
        }
        return alreadyGenerated
    }

    private fun generateConcepts() {
        val alreadyGenerated = mutableSetOf<String>()

        fun visitConcept(concept: GeneratedConcept) {
            if (concept.inner.name in alreadyGenerated) return
            alreadyGenerated.add(concept.inner.name)

            val parent = concept.inner.parent
            if (parent != null) {
                val parentConcept = input.concepts[parent] ?: error("Parent concept not found: $parent")
                visitConcept(parentConcept)
            }

            val type = generateConcept(concept)
            if (type is TypeAliasSpec) {
                file.addTypeAlias(type)
            } else if (type is TypeSpec) {
                file.addType(type)
            }
        }

        for (concept in input.concepts.values.sortedBy { it.inner.order }) {
            visitConcept(concept)
        }
    }

    private fun generateExtraSealedIntfs() {
        for ((_, name) in input.extraSealedIntfs) {
            val type = TypeSpec.interfaceBuilder(name).apply {
                addModifiers(KModifier.SEALED)
            }.build()
            file.addType(type)
        }
    }

    private fun generateDataRaw() {
        val allPrototypes = TypeSpec.classBuilder("DataRaw").apply {
            addKdoc("All prototypes, aka [data.raw](https://wiki.factorio.com/Data.raw). Only contains the subset of objects this library uses.")
            addAnnotation(Serializable::class)

            val constructorBuilder = FunSpec.constructorBuilder()
            for (prototype in input.prototypes.values) {
                val typeName = prototype.inner.typename ?: continue
                val mapType = Map::class.asClassName().parameterizedBy(
                    String::class.asClassName(),
                    ClassName(PACKAGE_NAME, prototype.inner.name)
                )
                addProperty(
                    PropertySpec.builder(typeName, mapType)
                        .initializer(typeName)
                        .build()
                )
                constructorBuilder.addParameter(typeName, mapType)
            }

            primaryConstructor(constructorBuilder.build())
        }.build()

        file.addType(allPrototypes)
    }


    private fun Documentable.Builder<*>.addDescription(description: String?) {
        if (!description.isNullOrBlank()) {
            addKdoc(description)
        }
    }

    private fun generatePrototype(prototype: GeneratedPrototype): TypeSpec =
        generateClass(prototype, prototype.inner.name)

    private fun generateClass(
        value: GeneratedValue,
        name: String,
        isDataClass: Boolean = false
    ): TypeSpec {
        val canBeObject = value.includedProperties.isEmpty()
                && value.inner.parent.let { it != null && input.concepts[it]?.includedProperties?.isEmpty() == true }
                && value.typeName != null
                && value.inner.name !in hasInheritors

        val builder = if (canBeObject) {
            TypeSpec.objectBuilder(name)
        } else {
            TypeSpec.classBuilder(name)
        }
        return builder.apply {
            // doc
            addDescription(value.inner.description)

            // annotations
            addAnnotation(Serializable::class)
            val typeName = value.typeName
            if (typeName != null) {
                addAnnotation(
                    AnnotationSpec.builder(SerialName::class)
                        .addMember("%S", typeName)
                        .build()
                )
            }

            // modifiers
            if (isDataClass) {
                addModifiers(KModifier.DATA)
            }

            val hasInheritors = value.inner.name in hasInheritors
            if (hasInheritors) {
                check(!isDataClass)
                addModifiers(KModifier.SEALED)
            }
            if (value.inner.abstract) check(hasInheritors)

            // supertypes
            val parent = value.inner.parent
            if (parent != null) {
                check(parent in input.prototypes || parent in input.concepts) {
                    "Parent prototype not found: $parent"
                }
                superclass(ClassName(PACKAGE_NAME, parent))
            }
            val extraSealedIntfs = extraSealedSupertypes[value.inner.name]
            if (extraSealedIntfs != null) {
                for (extra in extraSealedIntfs) {
                    addSuperinterface(ClassName(PACKAGE_NAME, extra))
                }
            }

            // properties
            if (isDataClass) {
                val constructorBuilder = FunSpec.constructorBuilder()
                for (property in value.includedProperties.values.sortedBy { it.inner.order }) {
                    val propertySpec = generateProperty(value, property, initByMutate = false) {
                        initializer(property.inner.name)
                    }
                    addProperty(propertySpec)
                    constructorBuilder.addParameter(propertySpec.name, propertySpec.type)
                }
                if (constructorBuilder.parameters.isNotEmpty())
                    primaryConstructor(constructorBuilder.build())
            } else {
                for (property in value.includedProperties.values.sortedBy { it.inner.order }) {
                    addProperty(generateProperty(value, property, initByMutate = true))
                }
            }
        }.build()
    }

    private fun Concept.canBeDataClass(): Boolean =
        !properties.isNullOrEmpty() && name !in hasInheritors

    private fun generateStructConcept(
        concept: GeneratedConcept,
        name: String,
    ): TypeSpec {
        return generateClass(concept, name, isDataClass = concept.inner.canBeDataClass())
    }

    private fun generateConcept(concept: GeneratedConcept): Any {
        val type = if (concept.overrideType != null) {
            GeneratedType(concept.overrideType, null)
        } else {
            mapTypeDefinition(concept.inner.type, concept, null, true)
        }
        return type.declaration
            ?: TypeAliasSpec.builder(concept.inner.name, type.putType()).apply {
                addDescription(concept.inner.description)
            }.build()
    }

    private fun getDefaultValue(
        property: Property,
    ): CodeBlock? {
        if (property.default is LiteralDefault) {
            val value = property.default.value
            val result: CodeBlock? = if (value.isString) CodeBlock.of("%S", value.content)
            else value.booleanOrNull?.let { CodeBlock.of("%L", it) }
                ?: value.longOrNull?.let { CodeBlock.of("%L", it) }
                ?: value.doubleOrNull?.let { CodeBlock.of("%L", it) }
            if(result!=null) {
                println("Wow, a default: $result")
            }
            return result
        }
        val simpleType = (property.type.innerType() as? BasicType)?.value
        return when {
            simpleType == null -> null
            simpleType == "double" -> CodeBlock.of("0.0")
            simpleType == "float" -> CodeBlock.of("0f")
            simpleType == "bool" -> CodeBlock.of("false")
            simpleType.contains("int") -> CodeBlock.of("0")
            else -> null
        }
    }

    private fun generateProperty(
        context: GeneratedValue,
        genProperty: GeneratedProperty,
        initByMutate: Boolean,
        block: PropertySpec.Builder.() -> Unit = {}
    ): PropertySpec {
        val property = genProperty.inner
        val nullable = property.optional || property.default != null

        val basicType =
            genProperty.overrideType ?: mapTypeDefinition(property.type, context, genProperty, true).putType()
        val type =
            basicType.copy(nullable = nullable)

        return PropertySpec.builder(property.name, type).apply {
            if (initByMutate) {
                mutable()
                if (nullable) {
                    initializer("null")
                } else {
                    val defaultValue = getDefaultValue(property)
                    if (defaultValue != null) {
                        initializer(defaultValue)
                    } else {
                        addModifiers(KModifier.LATEINIT)
                    }
                }
                setter(FunSpec.setterBuilder().addModifiers(KModifier.PRIVATE).build())
            }
            if (property.description.isNotBlank()) {
                addKdoc(property.description)
            }
            block()
        }.build()
    }


    private fun tryGetEnumOptions(type: TypeDefinition): List<LiteralType>? =
        if (type is UnionType && type.options.all { it is LiteralType && it.value.isString }) {
            @Suppress("UNCHECKED_CAST")
            type.options as List<LiteralType>
        } else {
            null
        }

    private fun generateEnumType(
        name: String,
        options: List<LiteralType>,
        block: TypeSpec.Builder.() -> Unit = {}
    ): TypeSpec =
        TypeSpec.enumBuilder(name).apply {
            addAnnotation(Serializable::class)

            for (option in options) {
                addEnumConstant(
                    option.value.content,
                    TypeSpec.anonymousClassBuilder().apply {
                        addDescription(option.description)
                    }.build()
                )
            }

            block()
        }.build()


    private fun tryGetItemOrArrayValue(
        type: UnionType
    ): TypeDefinition? {
        if (type.options.size != 2) return null
        val (first, second) = type.options
        if (second !is ArrayType || first != second.value) return null
        return first
    }

    private fun tryGetExtraSealedIntfName(type: UnionType): TypeName? {
        if (!type.options.all {
                it is BasicType
                        && it.value in input.concepts
            }) return null
        val options = type.options.map { (it as BasicType).value }.toSet()
        val name = input.extraSealedIntfs[options] ?: return null
        return ClassName(PACKAGE_NAME, name)
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
                if (property == null) {
                    if (isRoot) {
                        context.inner.name
                    } else {
                        context.innerEnumName ?: error("Inner enum name not specified for ${context.inner.name}")
                    }
                } else {
                    property.innerEnumName
                        ?: error("Inner enum name not specified for ${context.inner.name}.${property.inner.name}")
                }
            } else {
                error("todo")
            }
            val enumType = generateEnumType(name, options) {
                if (isRoot) {
                    addDescription(context.inner.description)
                }
            }
            GeneratedType(ClassName(PACKAGE_NAME, name), enumType)
        } ?: tryGetItemOrArrayValue(type)?.let { item ->
            ClassName(PACKAGE_NAME, "ItemOrArray")
                .parameterizedBy(mapTypeDefinition(item, context, property).putType())
                .toGenType()
        } ?: tryGetExtraSealedIntfName(type)?.toGenType()
        ?: run {
            println("UnionType $type not supported")
            Any::class.asClassName().toGenType()
        }
    }
}
