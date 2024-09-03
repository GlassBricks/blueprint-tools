package scripts

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.SpatialDataStructure
import glassbricks.factorio.blueprint.TileBoundingBox
import glassbricks.factorio.blueprint.entity.BlueprintEntity
import glassbricks.factorio.blueprint.toBoundingBox


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
            println("Exception: $e")
            return true
        }
        return false
    }
    if (!tryRun(curBp)) error("Initial run does not throw")
    while (true) {
        val quarter = curBounds.splitIntoQuarters().firstOrNull { tryRun(curBp.subset(it)) }
        if (quarter == null) {
            return curBp
        }
        curBounds = quarter
        curBp = curBp.subset(quarter)
        println(curBounds)
        println(curBp.size)
    }
}

fun TileBoundingBox.splitIntoQuarters(): List<TileBoundingBox> {
    val midX = (minX + maxXExclusive) / 2
    val midY = (minY + maxYExclusive) / 2
    return listOf(
        TileBoundingBox(minX, minY, midX, midY),
        TileBoundingBox(midX, minY, maxXExclusive, midY),
        TileBoundingBox(minX, midY, midX, maxYExclusive),
        TileBoundingBox(midX, midY, maxXExclusive, maxYExclusive),
    ).filter {
        it.isNotEmpty() && it != this
    }
}

fun SpatialDataStructure<BlueprintEntity>.subset(box: TileBoundingBox): SpatialDataStructure<BlueprintEntity> {
    val area = box.toBoundingBox()
    return DefaultSpatialDataStructure(filter { it.intersects(area) })
}
