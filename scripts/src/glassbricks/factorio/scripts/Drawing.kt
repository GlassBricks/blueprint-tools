package glassbricks.factorio.scripts

import com.github.nwillc.ksvg.RenderMode
import com.github.nwillc.ksvg.elements.SVG
import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.Spatial
import glassbricks.factorio.blueprint.entity.SpatialDataStructure
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
    fun draw(entity: Spatial, color: Color)
    fun drawEntity(entity: Spatial) = draw(entity, getEntityColor(entity))
    fun show(): Any
    fun saveTo(filename: String): Drawing
}

abstract class DrawingInBounds(
    protected val bounds: BoundingBox,
    imageHeight: Int,
) : Drawing {
    val imageWidth = (imageHeight * bounds.width / bounds.height).let { ceil(it).toInt() }
    val tileDistance = imageHeight / bounds.height
    fun toSvgPos(tile: Position): Pair<Double, Double> {
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
    return "#${red.toHex()}${green.toHex()}${blue.toHex()}"
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

    override fun draw(entity: Spatial, color: Color) {
        val collisionBox = entity.collisionBox.roundOutToTileBbox()
            .toBoundingBox()
        val (minX, minY) = toSvgPos(collisionBox.leftTop)
        val (maxX, maxY) = toSvgPos(collisionBox.rightBottom)

        svg.rect {
            x = minX.toString()
            y = minY.toString()
            width = (maxX - minX).toString()
            height = (maxY - minY).toString()
            fill = color.toSvgString()
            stroke = "black"
            strokeWidth = (0.1 * tileDistance).toString()
        }
    }

    override fun drawEntity(entity: Spatial) {
        val color = getEntityColor(entity)
        draw(entity, color)
    }

    override fun show() = svg.show()

    override fun saveTo(filename: String) = apply {
        val file = File("$filename.svg")
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

    override fun draw(entity: Spatial, color: Color) {
        val collisionBox = entity.collisionBox.roundOutToTileBbox()
            .toBoundingBox()
        val (minX, minY) = toSvgPos(collisionBox.leftTop)
        val (maxX, maxY) = toSvgPos(collisionBox.rightBottom)
        graphics.color = color
        val rect = Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY)
        graphics.fill(rect)
        graphics.color = Color.GRAY
        graphics.stroke = BasicStroke((0.1 * tileDistance).toFloat())
        graphics.draw(rect)
    }

    override fun show(): Any {
        return image
    }

    override fun saveTo(filename: String) = apply {
        val file = File("$filename.png")
        file.parentFile.mkdirs()
        ImageIO.write(image, "png", file)
    }
}

fun drawingFor(
    entities: SpatialDataStructure<Spatial>,
    height: Int? = null
): Drawing {
    val boundingBox = entities.enclosingBox().roundOutToTileBbox().toBoundingBox()

//    return SvgDrawing(boundingBox, height)
    return BufferedImageDrawing(boundingBox, height ?: ceil(boundingBox.height * 5.0).toInt())
}


fun Drawing.drawEntities(entities: Iterable<Spatial>): Drawing = apply {
    for (it in entities.sortedBy { it.position }) {
        drawEntity(it)
    }
}

fun drawEntities(entities: SpatialDataStructure<Spatial>): Drawing = drawingFor(entities).drawEntities(entities)
