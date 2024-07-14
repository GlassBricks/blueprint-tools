package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.json.EntityJson
import kotlin.reflect.full.declaredMemberProperties

class Gen(
    val name: String,
    vararg val props: String,
)

val gen = arrayOf(
    Gen("Container", "bar"),
)
val superTypes = arrayOf("WithBar, WithFilters")

fun main() {
    val superTypes = superTypes.joinToString("") {
        ",\n    $it"
    }
    for (gen in gen) {
        val Class = gen.name
        val (props, exports) = getProps(gen)
        // language=kotlin
        println(
            """
 class $Class
internal constructor(
    override val prototype: ${Class}Prototype,
    json: EntityInit<${Class}>,
) : BaseEntity(json)$superTypes {
    ${props.prependIndent("    ")}
    override fun exportToJson(json: EntityJson) {
        ${exports.prependIndent("        ")}
    }
""".trimIndent()
        )
    }
}

@Suppress("DEPRECATION")
private fun getProps(gen: Gen): Pair<String, String> {

    val props = gen.props.joinToString("\n") { prop ->
        val propCamel = prop.split("_").joinToString("") { it.capitalize() }
            .decapitalize()
        val type = EntityJson::class.declaredMemberProperties
            .find { it.name == prop }!!.returnType
        "public var $propCamel: $type = json.self?.$propCamel ?: json.json?.$prop" +
                (if (!type.isMarkedNullable) {
                    if (type.classifier == Boolean::class) " ?: false"
                    else " ?: TODO()"
                } else "")
    }
    val exports = gen.props.joinToString("\n") { prop ->
        val propCamel = prop.split("_").joinToString("") { it.capitalize() }
            .decapitalize()
        "json.$prop = $propCamel"
    }
    return props to exports
}
