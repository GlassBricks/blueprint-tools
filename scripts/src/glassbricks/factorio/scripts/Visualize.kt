package glassbricks.factorio.scripts

import glassbricks.factorio.blueprint.entity.Spatial
import java.awt.Color


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
        draw(entity, color)
    }
}
