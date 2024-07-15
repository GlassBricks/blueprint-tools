import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.Entity
import glassbricks.factorio.blueprint.poleopt.CandidatePole
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import org.apache.batik.dom.GenericDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.jetbrains.kotlinx.jupyter.ext.Image
import java.awt.Color
import java.awt.Dimension
import java.awt.geom.Rectangle2D
import java.io.ByteArrayOutputStream
import kotlin.math.ceil


fun makeSVGGraphics(): SVGGraphics2D {
    val dompImpl = GenericDOMImplementation.getDOMImplementation()
    val svgNS = "http://www.w3.org/2000/svg"
    val document = dompImpl.createDocument(svgNS, "svg", null)
    val svgGenerator = SVGGraphics2D(document)
    return svgGenerator
}

fun SVGGraphics2D.show(): Image {
    val useCSS = true
    val byteStream = ByteArrayOutputStream()
    val out = byteStream.writer()
    stream(out, useCSS)
    return Image(byteStream.toByteArray(), "svg")
}

class Drawing(
    val bounds: BoundingBox,
    val imageHeight: Int,
    val graphics: SVGGraphics2D = makeSVGGraphics(),
) {
    val imageWidth = imageHeight * bounds.width / bounds.height

    init {
        graphics.svgCanvasSize = Dimension(imageWidth.let(::ceil).toInt(), imageHeight)
    }

    fun toSvgPos(tile: Position): Pair<Double, Double> {
        val (posX, posY) = tile - bounds.leftTop
        val svgX = posX * imageWidth / bounds.width
        val svgY = posY * imageHeight / bounds.height
        return Pair(svgX, svgY)
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

        graphics.color = color
        val shape = Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY))
        graphics.fill(shape)
        if (outline) {
            graphics.color = Color.BLACK
            graphics.draw(shape)
        }
    }

    fun drawEntity(entity: Spatial) {
        val color = when {
            entity is Entity && entity.prototype is ElectricPolePrototype
                    || entity is CandidatePole -> Pallete.pole

            entity is Entity && entity.prototype.usesElectricity -> Pallete.powerable
            else -> Pallete.nonPowerable
        }
        draw(entity, color, true)
    }

    fun show() = graphics.show()
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
}
