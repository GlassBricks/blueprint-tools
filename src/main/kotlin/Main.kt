package org.example

import glassbricks.factorio.prototypes.loadFactorioPrototypesFromStream

private val dataRaw get() = object {}.javaClass.classLoader.getResource("data-raw-dump.json")!!

fun main() {
    val prototypeData = loadFactorioPrototypesFromStream(
        dataRaw.openStream()
    )
    println(prototypeData.item!!.values)
}
