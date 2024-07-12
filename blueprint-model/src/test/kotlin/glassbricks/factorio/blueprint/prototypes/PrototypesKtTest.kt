package glassbricks.factorio.blueprint.prototypes

import org.junit.jupiter.api.Test


class PrototypesKtTest {
    @Test
    fun canLoadPrototypes() {
        val dataRaw = this.javaClass.classLoader.getResourceAsStream("/data-raw-dump.json")!!.use {
            loadDataRawFromStream(it)
        }
        println(dataRaw)
    }
}
