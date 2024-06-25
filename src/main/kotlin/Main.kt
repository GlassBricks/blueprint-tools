package org.example

import glassbricks.factorio.prototypes.loadFactorioPrototypeDataFromStream
import kotlinx.serialization.ExperimentalSerializationApi

private val dataRaw get() = object {}.javaClass.classLoader.getResource("data-raw-dump.json")!!

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val prototypeData = loadFactorioPrototypeDataFromStream(
        dataRaw.openStream()
    )
    println(prototypeData.item!!.values)
}
