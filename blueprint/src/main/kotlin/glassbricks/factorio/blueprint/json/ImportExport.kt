package glassbricks.factorio.blueprint.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

public fun importBlueprint(string: String): ImportableBlueprint {
    return importBlueprintFromStream(string.byteInputStream())
}
public fun importBlueprintFromFile(file: File): ImportableBlueprint {
    return importBlueprintFromStream(file.inputStream())
}

internal fun <T> importBlueprint(string: String, serializer: KSerializer<T>): T =
    importBlueprintFromStream(string.byteInputStream(), serializer)

public fun importBlueprintFromStream(stream: InputStream): ImportableBlueprint {
    return importBlueprintFromStream(stream, ImportableBlueprint.serializer())
}

internal fun <T> importBlueprintFromStream(stream: InputStream, serializer: KSerializer<T>): T {
    val firstChar = skipWhitespaceTillFirstChar(stream)
    if (firstChar != '0') throw SerializationException("Invalid version identifier: $firstChar")
    return stream
        .let { Base64.getDecoder().wrap(it) }
        .let { InflaterInputStream(it) }
        .let {
            @OptIn(ExperimentalSerializationApi::class)
            bpJson.decodeFromStream(serializer, it)
        }
}


public fun exportBlueprint(bp: ImportableBlueprint): String {
    val writeStream = ByteArrayOutputStream()
    exportBlueprintToStream(bp, writeStream)
    return writeStream.toString()
}

public fun exportBlueprintToFile(bp: ImportableBlueprint, file: File) {
    exportBlueprintToStream(bp, file.outputStream())
}

public fun exportBlueprintToStream(bp: ImportableBlueprint, stream: OutputStream) {
    stream.write('0'.code)
    stream
        .let { Base64.getEncoder().wrap(it) }
        .let { DeflaterOutputStream(it) }
        .let {
            @OptIn(ExperimentalSerializationApi::class)
            bpJson.encodeToStream(ImportableBlueprint.serializer(), bp, it)
            it.close()
        }
}

private fun skipWhitespaceTillFirstChar(
    bytes: InputStream,
): Char {
    while (true) {
        val byte = bytes.read()
        if (byte == -1) throw SerializationException("Unexpected end of stream")
        val char = byte.toChar()
        if (!char.isWhitespace()) return char
    }

}
