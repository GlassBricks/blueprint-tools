package glassbricks.factorio.blueprint

import glassbricks.factorio.blueprint.entity.EntityJson
import kotlin.reflect.full.declaredMemberProperties

class Gen(
    val name: String,
    vararg val props: String,
)

val gen = arrayOf(
    Gen("TransportBelt"),
    Gen("Splitter", "input_priority", "output_priority", "filter"),
    Gen("Loader", "filters")
)
val superTypes = arrayOf("TransportBeltConnectable")

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
public class $Class
internal constructor(
    override val prototype: ${Class}Prototype,
    init: EntityInit<${Class}>,
) : BaseEntity(init)$superTypes {
    ${props.prependIndent("    ")}
    override fun exportToJson(json: EntityJson) {
        ${exports.prependIndent("        ")}
    }
    
    override fun copy(): $Class = ${Class}(prototype, copyInit(this))
}
""".trimIndent()
        )
    }
}

private fun getProps(gen: Gen): Pair<String, String> {
    
    val props = gen.props.joinToString("\n") { prop ->
        val propCamel = prop.split("_").joinToString("") { it.capitalize() }
            .decapitalize()
        val type = EntityJson::class.declaredMemberProperties
            .find { it.name == prop }!!.returnType
        "public var $propCamel: $type = init.self?.$propCamel ?: init.json?.$prop" +
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
