package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint
import glassbricks.factorio.blueprint.json.BlueprintJson
import glassbricks.factorio.blueprint.json.importBlueprint
import glassbricks.factorio.blueprintPrototypes
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class BlueprintJsonModelTest {

    fun assertBpMatches(
        bp: BlueprintJson,
        model: BlueprintModel
    ) {
        assertEquals(bp.label, model.label)
        assertEquals(bp.label_color, model.label_color)
        assertEquals(bp.description, model.description)
        assertEquals(bp.icons, model.icons)
        assertEquals(bp.snap_to_grid, model.snapToGridSettings?.snapToGrid)
        assertEquals(bp.absolute_snapping, model.snapToGridSettings?.positionRelativeToGrid != null)
        if (bp.absolute_snapping) {
            assertEquals(bp.position_relative_to_grid, model.snapToGridSettings?.positionRelativeToGrid)
        }
        assertEquals(bp.item, model.item)
        assertEquals(bp.version, model.version)

        assertEquals(bp.tiles, model.tiles.toTileList())
        assertEquals(bp.tiles.toTileMap(), model.tiles)
    }

    @Test
    fun canImport() {
        val bp = importBlueprint(blueprint("bp1.txt").inputStream())
        bp as BlueprintJson

        val model = BlueprintModel(blueprintPrototypes, bp)
        assertBpMatches(bp, model)
    }

}
