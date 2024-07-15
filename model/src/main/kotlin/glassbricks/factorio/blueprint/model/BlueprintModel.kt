package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.*
import glassbricks.factorio.blueprint.entity.*
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.BlueprintPrototypes

public class BlueprintModel
private constructor(
    public override var label: String? = null,
    public override var label_color: Color? = null,
    public override var description: String? = null,
    public val iconsArr: Array<SignalIDJson?>,
    public val tiles: TileMap = HashMap(),
    public val entities: MutableEntityMap = DefaultSpatialDataStructure(),
    public var snapToGridSettings: SnapToGridSettings? = null,
    public override var item: String = "blueprint",
    public override val version: FactorioVersion = FactorioVersion.DEFAULT
) : BlueprintItem {

    public constructor(
        prototypes: BlueprintPrototypes,
        blueprint: BlueprintJson
    ) : this(
        label = blueprint.label,
        label_color = blueprint.label_color,
        description = blueprint.description,
        iconsArr = arrayOfNulls<SignalIDJson>(4).apply {
            for (icon in blueprint.icons) {
                this[icon.index - 1] = icon.signal
            }
        },
        tiles = blueprint.tiles.toTileMap(),
        entities = DefaultSpatialDataStructure<Entity>().apply {
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

    public constructor() : this(iconsArr = arrayOfNulls(4), tiles = HashMap())

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

    public fun toBlueprint(): BlueprintJson = BlueprintJson(
        item = item,
        label = label,
        label_color = label_color,
        icons = icons,
        tiles = tiles.toTileList(),
        description = description,
        snap_to_grid = snapToGridSettings?.snapToGrid,
        absolute_snapping = snapToGridSettings?.positionRelativeToGrid != null,
        position_relative_to_grid = snapToGridSettings?.positionRelativeToGrid
            ?.takeUnless { it.isZero() }
    ).also { bp ->
        bp.setEntitiesFrom(entities.sortedBy { it.position })
    }

    public fun deepCopy(): BlueprintModel = BlueprintModel(
        label = label,
        label_color = label_color,
        description = description,
        iconsArr = iconsArr.copyOf(),
        tiles = tiles.toMutableMap(),
        entities = entities.copyEntities(),
        snapToGridSettings = snapToGridSettings?.copy(),
        item = item,
        version = version
    )

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
