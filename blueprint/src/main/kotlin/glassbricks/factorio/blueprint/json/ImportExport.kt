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

public fun importBlueprintString(string: String): Importable {
    return importBlueprintFrom(string.byteInputStream())
}

public fun importBlueprintFrom(file: File): Importable {
    return importBlueprintFrom(file.inputStream())
}

public fun importBlueprintFrom(stream: InputStream): Importable {
    return importBlueprintFrom(stream, Importable.serializer())
}

internal fun <T> importBlueprintFrom(stream: InputStream, serializer: KSerializer<T>): T {
    val firstChar = skipWhitespaceTillFirstChar(stream)
    if (firstChar != '0') throw SerializationException("Invalid version identifier: $firstChar")
    return stream
        .let { Base64.getMimeDecoder().wrap(it) }
        .let { InflaterInputStream(it) }
        .let {
            @OptIn(ExperimentalSerializationApi::class)
            bpJson.decodeFromStream(serializer, it)
        }
}


public fun Importable.exportToString(): String {
    val writeStream = ByteArrayOutputStream(1024)
    this.exportTo(writeStream)
    return writeStream.toString()
}

public fun Importable.exportTo(file: File) {
    file.parentFile.mkdirs()
    this.exportTo(file.outputStream())
}

public fun Importable.exportTo(stream: OutputStream) {
    stream.write('0'.code)
    stream
        .let { Base64.getEncoder().wrap(it) }
        .let { DeflaterOutputStream(it) }
        .use {
            @OptIn(ExperimentalSerializationApi::class)
            bpJson.encodeToStream(Importable.serializer(), this, it)
        }
}

private fun skipWhitespaceTillFirstChar(bytes: InputStream): Char {
    while (true) {
        val byte = bytes.read()
        if (byte == -1) throw SerializationException("Unexpected end of stream")
        val char = byte.toChar()
        if (!char.isWhitespace()) return char
    }
}
