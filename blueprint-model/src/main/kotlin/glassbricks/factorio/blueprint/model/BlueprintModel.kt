package glassbricks.factorio.blueprint.model

import glassbricks.factorio.blueprint.*

public class BlueprintModel
private constructor(
    public override var label: String? = null,
    public override var label_color: Color? = null,
    public override var description: String? = null,
    public val iconsArr: Array<SignalID?>,
    public val tiles: TileMap,
    public var snapToGridSettings: SnapToGridSettings? = null,
    public override var item: String = "blueprint",
    public override val version: FactorioVersion = FactorioVersion.DEFAULT
) : BlueprintItem {

    public constructor(blueprint: Blueprint) : this(
        label = blueprint.label,
        label_color = blueprint.label_color,
        description = blueprint.description,
        iconsArr = arrayOfNulls<SignalID>(4).apply {
            for (icon in blueprint.icons) {
                this[icon.index - 1] = icon.signal
            }
        },
        tiles = blueprint.tiles?.toTileMap() ?: HashMap(),
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

    private val entities: List<Entity> get() = emptyList()

    public fun defaultIcons(): List<Icon> {
        val item = entities.map { it.name }.mode() ?: tiles.values.mode() ?: this.item
        return listOf(Icon(1, SignalID(name = item, type = SignalType.Item)))
    }

    public fun toBlueprint(): Blueprint {
        return Blueprint(
            item = item,
            label = label,
            label_color = label_color,
            icons = icons,
            tiles = tiles.toTileList(),
            entities = null, // TODO,
            schedules = null, // TODO,
            description = description,
            snap_to_grid = snapToGridSettings?.snapToGrid,
            absolute_snapping = snapToGridSettings?.positionRelativeToGrid != null,
            position_relative_to_grid = snapToGridSettings?.positionRelativeToGrid
                ?.takeIf { !it.isZero() },
        )
    }

    public class SnapToGridSettings(
        /** The dimensions of the grid to snap to. */
        public var snapToGrid: TilePosition,
        /** If not null, then absolute snapping is used. */
        public var positionRelativeToGrid: TilePosition?,
    )
}

private fun <T> Iterable<T>.mode(): T? {
    // finds the k most common elements in the list
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
