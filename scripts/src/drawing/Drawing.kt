package drawing

import com.github.nwillc.ksvg.RenderMode
import com.github.nwillc.ksvg.elements.SVG
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.Spatial
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.placement.EntityPlacement
import glassbricks.factorio.blueprint.prototypes.ElectricPolePrototype
import glassbricks.factorio.blueprint.prototypes.usesElectricity
import org.jetbrains.kotlinx.jupyter.api.HTML
import org.jetbrains.kotlinx.jupyter.api.MimeTypedResult
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil

interface Drawing {
    fun drawRect(area: BoundingBox, color: Color, fill: Boolean = true)
    fun drawRect(rect: RotatedRectangle, color: Color)
    fun drawLine(start: Position, end: Position, color: Color)
    fun drawPoint(point: Position, color: Color)

    fun show(): Any
    fun saveTo(filename: String): Drawing
}

fun Drawing.drawRect(entity: Spatial, color: Color) {
    val collisionBox = entity.collisionBox.roundOutToTileBbox()
        .toBoundingBox()
    drawRect(collisionBox, color)
}

fun Drawing.drawEntity(entity: Spatial) = drawRect(entity, getEntityColor(entity))

fun Color.withAlpha(alpha: Int): Color = Color(red, green, blue, alpha)

abstract class DrawingInBounds(
    protected val bounds: BoundingBox,
    imageHeight: Int,
) : Drawing {
    val imageWidth = (imageHeight * bounds.width / bounds.height).let { ceil(it).toInt() }
    val tileDistance = imageHeight / bounds.height
    fun toImagePos(tile: Position): Pair<Double, Double> {
        val (posX, posY) = tile - bounds.leftTop
        return Pair(posX * tileDistance, posY * tileDistance)
    }
}

object Pallete {
    val other = Color(0x00, 0x60, 0x60)
    val powerable = Color(26, 255, 26)
    val smallPole = Color(255, 55, 55)
    val otherPole = Color(255, 100, 55)
    val candidatePole = Color(255, 255, 55)
    val otherCandidate = Color(55, 200, 200)
}

private fun getEntityColor(entity: Spatial): Color {
    val color = when {
        entity is EntityPlacement<*> && !entity.isFixed -> when {
            entity.prototype is ElectricPolePrototype -> Pallete.candidatePole
            else -> Pallete.otherCandidate
        }

        entity is Entity<*> && entity.prototype is ElectricPolePrototype -> if (entity.prototype == smallPole) Pallete.smallPole else Pallete.otherPole
        entity is Entity<*> && entity.prototype.usesElectricity -> Pallete.powerable
        else -> Pallete.other
    }
    return color
}

fun SVG.show(): MimeTypedResult {
    val str = buildString { render(this, RenderMode.INLINE) }
    return HTML(str)
}

fun SVG.saveTo(file: File) {
    file.bufferedWriter().use { out ->
        this.render(out, RenderMode.FILE)
    }
}

fun Int.toHex() = toString(16).padStart(2, '0')
fun Color.toSvgString(): String {
    if (alpha == 0) return "none"
    if (alpha == 255)
        return "#${red.toHex()}${green.toHex()}${blue.toHex()}"
    return "rgba(${red},${green},${blue},${alpha / 255.0})"
}

class SvgDrawing(
    bounds: BoundingBox,
    imageHeight: Int,
) : DrawingInBounds(bounds, imageHeight), Drawing {

    private val svg = SVG()

    init {
        svg.width = imageWidth.toString()
        svg.height = imageHeight.toString()
    }


    override fun drawRect(area: BoundingBox, color: Color, fill: Boolean) {
        val (minX, minY) = toImagePos(area.leftTop)
        val (maxX, maxY) = toImagePos(area.rightBottom)

        svg.rect {
            x = minX.toString()
            y = minY.toString()
            width = (maxX - minX).toString()
            height = (maxY - minY).toString()
            if (fill) this.fill = color.toSvgString()
        }
    }

    override fun drawRect(rect: RotatedRectangle, color: Color) {
        val points = rect.points.map { toImagePos(it) }
        svg.polygon {
            this.points = points.joinToString(" ") { (x, y) -> "$x,$y" }
            fill = color.toSvgString()
        }
    }

    override fun drawLine(
        start: Position,
        end: Position,
        color: Color,
    ) {
        val (startX, startY) = toImagePos(start)
        val (endX, endY) = toImagePos(end)

        svg.line {
            x1 = startX.toString()
            y1 = startY.toString()
            x2 = endX.toString()
            y2 = endY.toString()
            stroke = color.toSvgString()
            strokeWidth = (0.1 * tileDistance).toString()
        }
    }

    override fun drawPoint(point: Position, color: Color) {
        val (x, y) = toImagePos(point)
        svg.circle {
            cx = x.toString()
            cy = y.toString()
            r = (0.1 * tileDistance).toString()
            fill = color.toSvgString()
        }
    }

    override fun show() = svg.show()

    override fun saveTo(filename: String) = apply {
        val fileName = if (filename.endsWith(".svg")) filename else "$filename.svg"
        val file = File(fileName)
        file.parentFile.mkdirs()
        svg.saveTo(file)
    }
}

class BufferedImageDrawing(
    bounds: BoundingBox,
    imageHeight: Int,
) : DrawingInBounds(bounds, imageHeight), Drawing {
    private val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
    private val graphics: Graphics2D = image.createGraphics()

    init {

        // hint anti-aliasing
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }

    override fun drawRect(area: BoundingBox, color: Color, fill: Boolean) {
        val (minX, minY) = toImagePos(area.leftTop)
        val (maxX, maxY) = toImagePos(area.rightBottom)
        val rect = Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY)
        if (fill) {
            graphics.color = color
            graphics.fill(rect)
        }
        graphics.color = Color.GRAY
        graphics.stroke = BasicStroke((0.1 * tileDistance).toFloat())
        graphics.draw(rect)
    }

    override fun drawRect(rect: RotatedRectangle, color: Color) {
        TODO("Not yet implemented")
    }

    override fun drawLine(start: Position, end: Position, color: Color) {
        val (startX, startY) = toImagePos(start)
        val (endX, endY) = toImagePos(end)
        graphics.color = color
        graphics.stroke = BasicStroke((0.1 * tileDistance).toFloat())
        graphics.drawLine(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt())
    }

    override fun drawPoint(point: Position, color: Color) {
        val (x, y) = toImagePos(point)
        graphics.color = color
        graphics.fillOval(x.toInt(), y.toInt(), (0.1 * tileDistance).toInt(), (0.1 * tileDistance).toInt())
    }

    override fun show(): Any {
        return image
    }

    override fun saveTo(filename: String) = apply {
        val fileName = if (filename.endsWith(".png")) filename else "$filename.png"
        val file = File(fileName)
        file.parentFile.mkdirs()
        ImageIO.write(image, "png", file)
    }
}

fun drawingFor(
    entities: SpatialDataStructure<Spatial>,
    height: Int? = null,
): Drawing {
    val boundingBox = entities.enclosingBox()
        .roundOutToTileBbox()
        .expand(1)
        .toBoundingBox()

//    return SvgDrawing(boundingBox, height)
    return BufferedImageDrawing(boundingBox, height ?: ceil(boundingBox.height * 5.0).toInt())
}


fun Drawing.drawEntities(entities: Iterable<Spatial>): Drawing = apply {
    for (it in entities.sortedBy { it.position }) {
        drawEntity(it)
    }
}

fun drawEntities(entities: SpatialDataStructure<Spatial>): Drawing = drawingFor(entities).drawEntities(entities)

fun Drawing.drawHeatmap(entities: Map<out Spatial, Double>) {
    fun getColor(percentile: Double): Color {
        val hue = 0.5 - percentile * 0.5
        return Color.getHSBColor(hue.toFloat(), 1.0f, 1.0f)
    }
    if (entities.isEmpty()) return
    val min: Double = entities.values.min()
    val max: Double = entities.values.max()
    for ((entity, value) in entities) {
        val percentile = (value - min) / (max - min)
        val color = getColor(percentile)
        drawRect(entity, color)
    }
}
