package glassbricks.factorio.blueprint.prototypes

import kotlin.test.Test

class BlueprintPrototypesTest {
    @Test
    fun `can load blueprint prototypes`() {
        val dataRaw = BlueprintPrototypes.loadFromDataRaw(dataRawFile)
        println(dataRaw.blueprintableEntities)
    }
}
