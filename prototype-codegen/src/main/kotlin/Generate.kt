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

class PrototypeDeclarationsGenerator(val prototypes: GeneratedPrototypes) {

    private val file = FileSpec.builder(PACKAGE_NAME, "Prototypes")

    fun generate(): FileSpec {
        for (prototype in prototypes.prototypes) {
            val clazz = generatePrototype(prototype)
            file.addType(clazz)
        }
        return file.build()
    }

    private fun generatePrototype(prototype: GeneratedPrototype): TypeSpec =
        TypeSpec.interfaceBuilder(prototype.prototype.name).apply {
            for (property in prototype.includedProperties.values) {
                addProperty(generateProperty(property))
            }
        }.build()

    private fun generateProperty(genProperty: GeneratedProperty): PropertySpec {
        val property = genProperty.property
        val nullable = property.optional || property.default != null

        val type = mapTypeDefinition(property.type)
            .copy(nullable = nullable)

        return PropertySpec.builder(property.name, type).apply {
            if (property.description.isNotBlank()) {
                addKdoc(property.description)
            }
        }.build()
    }

    private fun mapTypeDefinition(type: TypeDefinition): TypeName = when (type) {
        is BasicType -> {
            val value = type.value
            predefined[value] ?: ClassName(PACKAGE_NAME, value)
        }

        is ArrayType -> List::class.asClassName()
            .parameterizedBy(mapTypeDefinition(type.value))

        is DictType -> Map::class.asClassName()
            .parameterizedBy(
                mapTypeDefinition(type.key),
                mapTypeDefinition(type.value)
            )

        is LiteralType -> {
            val value = type.value
            when {
                value.isString -> ClassName(PACKAGE_NAME, "UnknownStringLiteral")
                else -> error("Unhandled literal type: $value")
            }
        }

        is TypeType -> mapTypeDefinition(type.value)
        StructType -> error("StructType not supported")
        is TupleType -> error("TupleType not supported")
        is UnionType -> error("UnionType not supported")
    }
}
