package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.DefaultSpatialDataStructure
import glassbricks.factorio.blueprint.MutableSpatialDataStructure
import glassbricks.factorio.blueprint.SignalType
import glassbricks.factorio.blueprint.TilePosition
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes
import glassbricks.factorio.blueprint.prototypes.VanillaPrototypes
import java.io.File

public class Blueprint
public constructor(
    public override var label: String? = null,
    public override var label_color: Color? = null,
    public override var description: String? = null,
    public val iconsArr: Array<SignalIDJson?> = arrayOfNulls(4),
    public val tiles: TileMap = HashMap(),
    public var entities: MutableSpatialDataStructure<BlueprintEntity> = DefaultSpatialDataStructure(),
    public var snapToGridSettings: SnapToGridSettings? = null,
    public override var item: String = "blueprint",
    public override val version: FactorioVersion = FactorioVersion.DEFAULT,
) : BlueprintItem {

    public constructor(
        blueprint: Importable,
        prototypes: BlueprintPrototypes = VanillaPrototypes,
    ) : this(
        label = (blueprint as BlueprintJson).label,
        label_color = blueprint.label_color,
        description = blueprint.description,
        iconsArr = arrayOfNulls<SignalIDJson>(4).apply {
            for (icon in blueprint.icons) {
                this[icon.index - 1] = icon.signal
            }
        },
        tiles = blueprint.tiles.toTileMap(),
        entities = DefaultSpatialDataStructure<BlueprintEntity>().apply {
            addAll(prototypes.entitiesFromJson(blueprint))
        },
        snapToGridSettings = blueprint.snap_to_grid?.let {
            SnapToGridSettings(
                it,
                blueprint.position_relative_to_grid.takeIf { blueprint.absolute_snapping },
            )
        },
        item = blueprint.item,
        version = blueprint.version,
    )

    public constructor(
        file: File,
        prototypes: BlueprintPrototypes = VanillaPrototypes,
    ) : this(importBlueprintFrom(file), prototypes)

    override val icons: List<Icon>
        get() {
            val signalCount = iconsArr.count { it != null }
            return if (signalCount == 0) defaultIcons()
            else ArrayList<Icon>(signalCount).apply {
                for (i in iconsArr.indices) {
                    val signal = iconsArr[i] ?: continue
                    add(Icon(i + 1, signal))
                }
            }
        }

    public fun defaultIcons(): List<Icon> {
        val item = entities.map { it.name }.mode() ?: tiles.values.mode() ?: this.item
        return listOf(Icon(1, SignalIDJson(name = item, type = SignalType.item)))
    }

    public fun toJson(): BlueprintJson = BlueprintJson(
        item = item,
        label = label,
        label_color = label_color,
        icons = icons,
        tiles = tiles.toTileList(),
        description = description,
        snap_to_grid = snapToGridSettings?.snapToGrid,
        absolute_snapping = snapToGridSettings?.positionRelativeToGrid != null,
        position_relative_to_grid = snapToGridSettings?.positionRelativeToGrid ?: TilePosition.ZERO
    ).also { bp ->
        bp.setEntitiesFrom(entities.sortedBy { it.position })
    }

    public fun deepCopy(): Blueprint = Blueprint(
        label = label,
        label_color = label_color,
        description = description,
        iconsArr = iconsArr.copyOf(),
        tiles = tiles.toMutableMap(),
        entities = entities.copyEntitiesSpatial(),
        snapToGridSettings = snapToGridSettings?.copy(),
        item = item,
        version = version
    )

    override fun toString(): String = "BlueprintModel(label=$label, ${entities.size} entities, ${tiles.size} tiles)"

}

public data class SnapToGridSettings(
    /** The dimensions of the grid to snap to. */
    public val snapToGrid: TilePosition,
    /** If not null, then absolute snapping is used. */
    public val positionRelativeToGrid: TilePosition?,
)

// statistical mode
private fun <T> Iterable<T>.mode(): T? {
    val counts = mutableMapOf<T, Int>()
    var mode: T? = null
    for (element in this) {
        val newCount = counts.getOrDefault(element, 0) + 1
        counts[element] = newCount
        if (newCount > counts.getOrDefault(mode, 0)) {
            mode = element
        }
    }
    return mode
}
