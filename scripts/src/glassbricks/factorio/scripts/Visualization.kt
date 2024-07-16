package glassbricks.factorio.scripts

import com.github.nwillc.ksvg.RenderMode
import com.github.nwillc.ksvg.elements.SVG
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.entity.Spatial
import glassbricks.factorio.blueprint.entity.SpatialDataStructure
import glassbricks.factorio.blueprint.poleopt.CandidatePole
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import org.jetbrains.kotlinx.jupyter.api.HTML
import org.jetbrains.kotlinx.jupyter.api.MimeTypedResult
import java.awt.Color

fun SVG.show(): MimeTypedResult {
    val str = buildString { render(this, RenderMode.INLINE) }
    return HTML(str)
}

fun Int.toHex() = toString(16).padStart(2, '0')
fun Color.toSvgString(): String {
    return "#${red.toHex()}${green.toHex()}${blue.toHex()}"
}

class Drawing(
    val bounds: BoundingBox,
    val imageHeight: Int,
) {
    val imageWidth = imageHeight * bounds.width / bounds.height

    val svg = SVG()

    init {
        svg.width = imageWidth.toString()
        svg.height = imageHeight.toString()
    }

    val tileDistance = imageHeight / bounds.height

    fun toSvgPos(tile: Position): Pair<Double, Double> {
        val (posX, posY) = tile - bounds.leftTop
        return Pair(posX * tileDistance, posY * tileDistance)
    }

    fun draw(
        spatial: Spatial,
        color: Color,
        outline: Boolean = true,
    ) {
        val collisionBox = spatial.collisionBox.roundOutToTileBbox()
            .toBoundingBox()
        val (minX, minY) = toSvgPos(collisionBox.leftTop)
        val (maxX, maxY) = toSvgPos(collisionBox.rightBottom)

        svg.rect {
            x = minX.toString()
            y = minY.toString()
            width = (maxX - minX).toString()
            height = (maxY - minY).toString()
            fill = color.toSvgString()
            if (outline) {
                stroke = "black"
                strokeWidth = (0.1 * tileDistance).toString()
            }
        }
    }

    fun drawEntity(entity: Spatial) {
        val color = when {
            entity is CandidatePole -> Pallete.candidatePole
            entity is Entity && entity.prototype is ElectricPolePrototype -> Pallete.pole
            entity is Entity && entity.prototype.usesElectricity -> Pallete.powerable
            else -> Pallete.nonPowerable
        }
        draw(entity, color, true)
    }

    fun show() = svg.show()
}

fun drawingFor(
    entities: SpatialDataStructure<Spatial>,
    height: Int = 500,
): Drawing {
    val boundingBox = entities.enclosingBox().roundOutToTileBbox().toBoundingBox()

    return Drawing(boundingBox, height)
}

fun drawEntities(
    entities: SpatialDataStructure<Spatial>,
    height: Int = 500,
): Drawing {
    val drawing = drawingFor(entities, height)
    for (it in entities) {
        drawing.drawEntity(it)

        // this can take a while, so graceful exit
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
    }
    return drawing
}

object Pallete {
    val nonPowerable = Color(0x00, 0x60, 0x60, 0x80)
    val powerable = Color(26, 255, 26)
    val pole = Color(255, 55, 55)
    val candidatePole = Color(255, 255, 0)
}
