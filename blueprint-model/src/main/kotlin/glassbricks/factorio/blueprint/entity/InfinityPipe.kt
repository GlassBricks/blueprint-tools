package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.InfinityFilterMode
import glassbricks.factorio.blueprint.json.InfinitySettings
import glassbricks.factorio.prototypes.InfinityPipePrototype
import kotlin.math.roundToInt


public class InfinityPipe(
    override val prototype: InfinityPipePrototype,
    json: EntityJson,
): BaseEntity(json) {
    public var infinitySettings: InfinityPipeSettings? = json.infinity_settings?.let {
        val name = it.name ?: return@let null
        val percentage = it.percentage ?: 1.0
        val mode = it.mode ?: InfinityFilterMode.AtLeast
        val temperature = it.temperature ?: 15
        InfinityPipeSettings(name, mode, (percentage * 100).roundToInt(), temperature)
    }

    override fun exportToJson(json: EntityJson) {
        json.infinity_settings = infinitySettings?.let {
            InfinitySettings(
                name = it.name,
                mode = it.mode,
                percentage = it.percentage / 100.0,
                temperature = it.temperature,
            )
        }
    }
}

public data class InfinityPipeSettings(
    /** The name of the fluid in the pipe */
    val name: String,
    val mode: InfinityFilterMode,
    /** The percentage fill threshold for the pipe. */
    val percentage: Int,
    /** The temperature of the fluid in the pipe. */
    val temperature: Int,
)
