import drawing.SvgDrawing
import drawing.drawingFor
import drawing.withAlpha
import glassbricks.factorio.blueprint.BoundingBox
import glassbricks.factorio.blueprint.Direction
import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.entity.Rail
import glassbricks.factorio.blueprint.entity.createBpEntity
import glassbricks.factorio.blueprint.json.importBlueprintFrom
import glassbricks.factorio.blueprint.model.Blueprint
import glassbricks.factorio.blueprint.pos
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import glassbricks.factorio.blueprint.roundOutToTileBbox
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.awt.Color
import java.io.File

class DrawRails {

    @ParameterizedTest
    @EnumSource(Direction::class)
    fun `draw rail bboxes`(direction: Direction) {
        for (proto in listOf("straight-rail", "curved-rail")) {
            val rail =
                VanillaPrototypes.createBpEntity<Rail>(proto, pos(2.0, 3.0), direction)
            val primary = rail.getAABBBox()
            val secondary = rail.getRectangleBox()

            fun hitsPoint(point: Position): Boolean {
                return primary?.contains(point) == true || secondary?.contains(point) == true
            }

            val bounds = BoundingBox.around(rail.position, 4.0)
            val drawing = SvgDrawing(
                bounds,
                200
            )
            for (tile in bounds.roundOutToTileBbox()) {
                val pt = tile.tileTopLeft()
                val color = if (hitsPoint(pt)) Color.RED else Color.BLACK
                drawing.drawPoint(pt, color)

                val area = tile.tileBoundingBox()
                if (rail.intersects(area)) {
                    drawing.drawRect(area, Color.GREEN.withAlpha(100))
                }
            }


            drawing.drawPoint(rail.position, Color.GREEN)

            primary?.let {
                drawing.drawRect(it, Color.BLUE.withAlpha(100))
            }
            secondary?.let {
                drawing.drawRect(it, Color.RED.withAlpha(100))
            }
            drawing.saveTo("./drawings/$proto-${direction}.svg")
        }
    }

    @Test
    fun `draw rail blob`() {
        val bp = Blueprint(importBlueprintFrom(File("../test-blueprints/rail-blob.txt")))
        val entities = bp.entities
        entities.retainAll { it is Rail }
        val drawing = drawingFor(entities)
        for (tile in entities.enclosingTileBox()) {
            val testArea = BoundingBox.around(tile.tileCenter(), 0.3)
            val hasStuff = entities.getInArea(testArea).any()
            if (hasStuff) {
                drawing.drawRect(testArea, Color.RED)
            }
        }
        drawing.saveTo("./drawings/rail-blob.png")
    }
}
