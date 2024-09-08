package scripts

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.TileBoundingBox
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.toBoundingBox
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor


fun getClipboard(): String = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String

fun bisectBp(
    bp: SpatialDataStructure<BlueprintEntity>,
    run: (SpatialDataStructure<BlueprintEntity>) -> Unit,
): SpatialDataStructure<BlueprintEntity> {
    var curBounds = bp.enclosingTileBox()
    var curBp = bp
    fun tryRun(entities: SpatialDataStructure<BlueprintEntity>): Boolean {
        try {
            run(entities)
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
        return false
    }
    if (!tryRun(curBp)) error("Initial run does not throw")
    while (true) {
        val quarter = curBounds.split().firstOrNull { tryRun(curBp.subset(it)) }
        if (quarter == null) {
            return curBp
        }
        curBounds = quarter
        curBp = curBp.subset(quarter)
        println(curBounds)
        println(curBp.size)
    }
}

fun TileBoundingBox.split(): List<TileBoundingBox> {
    val midX = (minX + maxXExclusive) / 2
    val midY = (minY + maxYExclusive) / 2
    val xQuartile = minX + (maxXExclusive - minX) / 4
    val yQuartile = minY + (maxYExclusive - minY) / 4
    val xQuartile3 = minX + 3 * (maxXExclusive - minX) / 4
    val yQuartile3 = minY + 3 * (maxYExclusive - minY) / 4
    return listOf(
        // cut on x only
        TileBoundingBox(minX, minY, maxXExclusive, midY),
        TileBoundingBox(minX, midY, maxXExclusive, maxYExclusive),
        // cut on y only
        TileBoundingBox(minX, minY, midX, maxYExclusive),
        TileBoundingBox(midX, minY, maxXExclusive, maxYExclusive),
        // cut a quarter of x
        TileBoundingBox(minX, minY, xQuartile3, maxYExclusive),
        TileBoundingBox(xQuartile, minY, maxXExclusive, maxYExclusive),
        // cut a quarter of y
        TileBoundingBox(minX, minY, maxXExclusive, yQuartile3),
        TileBoundingBox(minX, yQuartile, maxXExclusive, maxYExclusive),
        // cut 1 tile on each side
        TileBoundingBox(minX + 1, minY, maxXExclusive, maxYExclusive),
        TileBoundingBox(minX, minY + 1, maxXExclusive, maxYExclusive),
        TileBoundingBox(minX, minY, maxXExclusive - 1, maxYExclusive),
        TileBoundingBox(minX, minY, maxXExclusive, maxYExclusive - 1),
    ).filter {
        it.isNotEmpty() && it != this
    }
}

fun SpatialDataStructure<BlueprintEntity>.subset(box: TileBoundingBox): SpatialDataStructure<BlueprintEntity> {
    val area = box.toBoundingBox()
    return DefaultSpatialDataStructure(filter { it.intersects(area) })
}
