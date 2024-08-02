package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import kotlin.test.Test
import kotlin.test.fail

class AllPrototypesLoadedTest {
    @Test
    fun `all prototypes are matched`() {
        val unknownKeys = VanillaPrototypes.blueprintableEntities.entries
            .filter {
                VanillaPrototypes.createBpEntity<BlueprintEntity>(
                    name = it.key,
                    position = Position.ZERO
                ) is UnknownEntity
            }

        if (unknownKeys.isNotEmpty()) {
            val classList = unknownKeys.joinToString("\n") {
                "  ${it.key}: ${it.value.type}, ${it.value.flags}"
            }
            fail("The following prototypes are not matched:\n$classList")
        }
    }
}
