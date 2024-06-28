@file:Suppress("PropertyName")
@file:OptIn(ExperimentalSerializationApi::class)

package glassbricks.factorio.blueprint.json

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject


public typealias ItemPrototypeName = String

public interface BlueprintItem {
    /** The name of the item that was saved */
    public val item: ItemPrototypeName

    /** The name of the blueprint set by the user. */
    public val label: String?

    /** The color of the label of this blueprint. */
    public val label_color: Color?

    /** The icons of the blueprint set by the user. */
    public val icons: List<Icon>

    /** The description of the blueprint. */
    public val description: String?

    /** The map version of the map the blueprint was created in. */
    public val version: FactorioVersion
}


@Serializable(with = BlueprintItemSerializer::class)
public sealed interface ImportableBlueprint : BlueprintItem

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Blueprint_book_object)
 */
@Serializable
public data class BlueprintBook(
    /** The name of the item that was saved ("blueprint-book" in vanilla). */
    public override var item: ItemPrototypeName = "blueprint-book",
    /** The name of the blueprint set by the user. */
    public override var label: String? = null,
    /** The color of the label of this blueprint. */
    public override var label_color: Color? = null,
    /** The actual content of the blueprint book. */
    public var blueprints: List<BlueprintIndex> = emptyList(),
    /** Index of the currently selected blueprint, 0-based. */
    public var active_index: Int = 0,
    /** The icons of the blueprint book set by the user. */
    public override var icons: List<Icon> = emptyList(),
    /** The description of the blueprint book. */
    public override var description: String? = null,
    /** The map version of the map the blueprint was created in. */
    public override var version: FactorioVersion = FactorioVersion.DEFAULT,
) : ImportableBlueprint {
    override fun toString(): String = toStringImpl("BlueprintBook")
}

@Serializable
public data class BlueprintIndex(
    /** 0-based index */
    public val index: Int,
    public val blueprint: Blueprint,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Blueprint_object)
 */
@Serializable
public data class Blueprint(
    /** The name of the item that was saved ("blueprint" in vanilla). */
    public override var item: ItemPrototypeName = "blueprint",
    /** The name of the blueprint set by the user. */
    public override var label: String? = null,
    /** The color of the label of this blueprint. */
    public override var label_color: Color? = null,
    /** The actual content of the blueprint. */
    public var entities: List<Entity>? = null,
    /** The tiles included in the blueprint. */
    public var tiles: List<Tile>? = null,
    /** The icons of the blueprint set by the user. */
    public override var icons: List<Icon>,
    /** The schedules for trains in this blueprint. */
    public var schedules: List<Schedule>? = null,
    /** The description of the blueprint. */
    public override var description: String? = null,
    /** The dimensions of the grid to use for snapping. */
    @SerialName("snap-to-grid")
    public var snap_to_grid: TilePosition? = null,
    /** Whether the blueprint uses absolute or relative snapping. */
    @SerialName("absolute-snapping")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var absolute_snapping: Boolean = false,
    /** Offset relative to the global absolute snapping grid. */
    @SerialName("position-relative-to-grid")
    public var position_relative_to_grid: TilePosition? = null,
    /** The map version of the map the blueprint was created in. */
    public override var version: FactorioVersion = FactorioVersion.DEFAULT,
) : ImportableBlueprint {
    override fun toString(): String = toStringImpl("Blueprint")
}

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Icon_object)
 */
@Serializable
public data class Icon(
    /** Index of the icon, 1-based. */
    public val index: Int,
    /** The icon that is displayed. */
    public val signal: SignalID,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#SignalID_object)
 */
@Serializable
public data class SignalID(
    /**
     * Name of the signal prototype this signal is set to.
     *
     * In roboport read robot count, this may be null to indicate no signal (instead of the default signal).
     */
    public val name: String?,
    /** Type of the signal. Either "item", "fluid" or "virtual". */
    public val type: SignalType,
)


@Serializable
public enum class SignalType {
    @SerialName("item")
    Item,

    @SerialName("fluid")
    Fluid,

    @SerialName("virtual")
    Virtual
}

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
@Serializable
@JvmInline
public value class EntityNumber(public val id: Int) {
    init {
        require(id >= 1) { "EntityId must be a 1-based index" }
    }
}

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Entity_object)
 */
@Serializable
public data class Entity(
    /** Index of the entity, 1-based. */
    public var entity_number: EntityNumber,
    /** Prototype name of the entity (e.g. "offshore-pump"). */
    public var name: String,
    /** Position of the entity within the blueprint. */
    public var position: Position,
    /** Direction of the entity. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var direction: Direction = Direction.North,
    /** Orientation of cargo wagon or locomotive. */
    public var orientation: DoubleAsInt? = null,
    /** Circuit connection. */
    public var connections: Connections? = null,
    /** Copper wire connections. */
    public var neighbours: List<EntityNumber>? = null,
    /** Control behavior of this entity. */
    public var control_behavior: ControlBehavior? = null,
    /** Item requests by this entity. */
    public var items: ItemRequests? = null,
    /** Name of the recipe prototype this assembling machine is set to. */
    public var recipe: String? = null,
    /** Used by [Prototype/Container](https://wiki.factorio.com/Prototype/Container). */
    public var bar: Int? = null,
    /** Cargo wagon inventory configuration. */
    public var inventory: Inventory? = null,
    /** Used by [Prototype/InfinityContainer](https://wiki.factorio.com/Prototype/InfinityContainer). */
    public var infinity_settings: InfinitySettings? = null,
    /** Type of the underground belt or loader. Either "input" or "output". */
    public var type: IOType? = null,
    /** Input priority of the splitter. Either "right" or "left". */
    public var input_priority: SplitterPriority? = null,
    /** Output priority of the splitter. Either "right" or "left". */
    public var output_priority: SplitterPriority? = null,
    /** Filter of the splitter. Name of the item prototype the filter is set to. */
    public var filter: String? = null,
    /** Filters of the filter inserter or loader. */
    public var filters: List<ItemFilter>? = null,
    /** Filter mode of the filter inserter. Either "whitelist" or "blacklist". */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var filter_mode: FilterMode = FilterMode.Whitelist,
    /** The stack size the inserter is set to. */
    public var override_stack_size: UByte? = null,
    /** The drop position the inserter is set to. */
    public var drop_position: Position? = null,
    /** The pickup position the inserter is set to. */
    public var pickup_position: Position? = null,
    /** Used by [Prototype/LogisticContainer](https://wiki.factorio.com/Prototype/LogisticContainer). */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var request_filters: List<LogisticFilter> = emptyList(),
    /** Whether this requester chest can request from buffer chests. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var request_from_buffers: Boolean = false,
    /** Used by [Programmable speaker](https://wiki.factorio.com/Programmable_speaker). */
    public var parameters: SpeakerParameters? = null,
    /** Used by [Programmable speaker](https://wiki.factorio.com/Programmable_speaker). */
    public var alert_parameters: AlertParameters? = null,
    /** Used by the rocket silo. Whether auto launch is enabled. */
    public var auto_launch: Boolean? = null,
    /** Used by [Prototype/SimpleEntityWithForce](https://wiki.factorio.com/Prototype/SimpleEntityWithForce) or [Prototype/SimpleEntityWithOwner](https://wiki.factorio.com/Prototype/SimpleEntityWithOwner). */
    public var variation: UByte? = null,
    /** Color of the [Prototype/SimpleEntityWithForce](https://wiki.factorio.com/Prototype/SimpleEntityWithForce), [Prototype/SimpleEntityWithOwner](https://wiki.factorio.com/Prototype/SimpleEntityWithOwner), or train station. */
    public var color: Color? = null,
    /** The name of the train station. */
    public var station: String? = null,
    /** The manually set train limit of the train station. */
    public var manual_trains_limit: Int? = null,
    /** The current state of the power switch. */
    public var switch_state: Boolean? = null,
    /** Dictionary of arbitrary data. */
    public var tags: JsonObject? = null,
)


@Serializable
public enum class IOType {
    @SerialName("input")
    Input,

    @SerialName("output")
    Output,
}

public fun IOType.flip(): IOType = when (this) {
    IOType.Input -> IOType.Output
    IOType.Output -> IOType.Input
}

@Serializable
public enum class SplitterPriority {
    @SerialName("left")
    Left,

    @SerialName("right")
    Right,
}

@Serializable
public enum class FilterMode {
    @SerialName("whitelist")
    Whitelist,

    @SerialName("blacklist")
    Blacklist,
}

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Inventory_object)
 */
@Serializable
public data class Inventory(
    /** Array of item filters. */
    public var filters: List<ItemFilter> = emptyList(),
    /** The index of the first inaccessible item slot due to limiting with the red "bar". */
    public var bar: Int? = null,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Schedule_object)
 */
@Serializable
public data class Schedule(
    /** Array of schedule records. */
    public var schedule: List<ScheduleRecord>,
    /** Array of entity numbers of locomotives using this schedule. */
    public var locomotives: List<EntityNumber>,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Schedule_Record_object)
 */
@Serializable
public data class ScheduleRecord(
    /** The name of the stop for this schedule record. */
    public var station: String,
    /** Array of wait conditions. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var wait_conditions: List<WaitCondition> = emptyList(),
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Wait_Condition_object)
 */
@Serializable
public data class WaitCondition(
    /** One of "time", "inactivity", "full", "empty", "item_count", "circuit", "robots_inactive", "fluid_count", "passenger_present", "passenger_not_present". */
    public var type: WaitConditionType,
    /** Either "and" or "or". Tells how this condition is to be compared with the preceding conditions in the corresponding wait_conditions array. */
    public var compare_type: CompareType = CompareType.Or,
    /** Number of ticks to wait or of inactivity. Only present when type is "time" or "inactivity". */
    public var ticks: Int? = null,
    /** Circuit condition, only present when type is "item_count", "circuit" or "fluid_count". */
    public var condition: CircuitCondition? = null,
)

@Serializable
public enum class WaitConditionType {
    @SerialName("time")
    Time,

    @SerialName("inactivity")
    Inactivity,

    @SerialName("full")
    Full,

    @SerialName("empty")
    Empty,

    @SerialName("item_count")
    ItemCount,

    @SerialName("circuit")
    Circuit,

    @SerialName("robots_inactive")
    RobotsInactive,

    @SerialName("fluid_count")
    FluidCount,

    @SerialName("passenger_present")
    PassengerPresent,

    @SerialName("passenger_not_present")
    PassengerNotPresent,
}

@Serializable
public enum class CompareType {
    @SerialName("or")
    Or,

    @SerialName("and")
    And,
}

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Tile_object)
 */
@Serializable
public data class Tile(
    /** Prototype name of the tile (e.g. "concrete"). */
    public val name: String,
    /** Position of the entity within the blueprint. */
    public var position: TilePosition,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Connection_object)
 */
@Serializable
public data class Connections(
    /** First connection point. The default for everything that doesn't have multiple connection points. */
    public var `1`: ConnectionPoint? = null,
    /** Second connection point. For example, the "output" part of an arithmetic combinator. */
    public var `2`: ConnectionPoint? = null,
    /** Only used by power switches */
    public var Cu0: List<CableConnectionData>? = null,
    /** Only used by power switches */
    public var Cu1: List<CableConnectionData>? = null,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Connection_point_object)
 */
@Serializable
public data class ConnectionPoint(
    /** An array of connection data objects containing all the connections from this point created by red wire. */
    public var red: List<ConnectionData>? = null,
    /** An array of connection data objects containing all the connections from this point created by green wire. */
    public var green: List<ConnectionData>? = null,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Connection_data_object)
 */
@Serializable
public data class ConnectionData(
    /** ID of the entity this connection is connected with. */
    public val entity_id: EntityNumber,
    /** The circuit connector id of the entity this connection is connected to. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val circuit_id: CircuitID = CircuitID.First,
)

/**
 * Only used when a power switch connects to a pole. Contains the [EntityNumber] of the pole.
 */
@Serializable
public data class CableConnectionData internal constructor(
    public val entity_id: EntityNumber,
    /** Always 0. */
    public val circuit_id: Int = 0,
) {
    public constructor(entityId: EntityNumber) : this(entityId, 0)
}

@Serializable(with = CircuitID.Serializer::class)
public enum class CircuitID {
    First,
    Second;

    internal object Serializer : EnumOrdinalSerializer<CircuitID>(CircuitID::class, offset = 1)
}

/**
 * 1 or more instances of key/value pairs. Key is the name of the item, string. Value is the amount of items to be requested, Types/ItemCountType.
 *
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Item_request_object)
 */
public typealias ItemRequests = Map<ItemPrototypeName, Int>

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Item_filter_object)
 */
@Serializable
public data class ItemFilter(
    /** Name of the item prototype this filter is set to. */
    public val name: ItemPrototypeName,
    /** Index of the filter, 1-based. */
    public val index: Int,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Infinity_settings_object)
 */
@Serializable
public data class InfinitySettings(
    /** Whether the "remove unfiltered items" checkbox is checked. */
    public val remove_unfiltered_items: Boolean = false,
    /** Filters of the infinity container. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val filters: List<InfinityFilter> = emptyList(),
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Infinity_filter_object)
 */
@Serializable
public data class InfinityFilter(
    /** Name of the item prototype the filter is set to. */
    public val name: ItemPrototypeName,
    /** Number the filter is set to. */
    public val count: Int,
    /** Mode of the filter. Either "at-least", "at-most", or "exactly". */
    public val mode: InfinityFilterMode,
    /** Index of the filter, 1-based. */
    public val index: Int,
)


@Serializable
public enum class InfinityFilterMode {
    @SerialName("at-least")
    AtLeast,

    @SerialName("at-most")
    AtMost,

    @SerialName("exactly")
    Exactly,
}

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Logistic_filter_object)
 */
@Serializable
public data class LogisticFilter(
    /** Name of the item prototype this filter is set to. */
    public val name: ItemPrototypeName,
    /** Index of the filter, 1-based. */
    public val index: Int,
    /** Number the filter is set to. */
    public val count: Int,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Speaker_parameter_object)
 */
@Serializable
public data class SpeakerParameters(
    /** Volume of the speaker. */
    public val playback_volume: DoubleAsInt,
    /** Whether global playback is enabled. */
    public val playback_globally: Boolean,
    /** Whether polyphony is allowed. */
    public val allow_polyphony: Boolean,
)

/**
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Speaker_alert_parameter_object)
 */
@Serializable
public data class AlertParameters(
    /** Whether an alert is shown. */
    public val show_alert: Boolean = false,
    /** Whether an alert icon is shown on the map. */
    public val show_on_map: Boolean = false,
    /** The icon that is displayed with the alert. */
    public val icon_signal_id: SignalID? = null,
    /** Message of the alert. */
    public val alert_message: String = "",
)

@Serializable
public data class ProgrammableSpeakerCircuitParameters(
    public val signal_value_is_pitch: Boolean,
    public val instrument_id: Int,
    public val note_id: Int,
)


/** [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Color_object) */
@Serializable
public data class Color(
    /** Red, number from 0 to 1. */
    public val r: Float,
    /** Green, number from 0 to 1. */
    public val g: Float,
    /** Blue, number from 0 to 1. */
    public val b: Float,
    /** Alpha, number from 0 to 1. */
    public val a: Float = 1.0f,
) {
    public constructor(r: Double, g: Double, b: Double, a: Double = 1.0) : this(
        r.toFloat(),
        g.toFloat(),
        b.toFloat(),
        a.toFloat()
    )
}

/**
 * ALL fields are optional and depend on the type of the entity.
 *
 * [Online Documentation](https://wiki.factorio.com/Blueprint_string_format#Control_behavior_object)
 */
@Serializable
public data class ControlBehavior(
    public var logistic_condition: CircuitCondition? = null,
    /** Whether this entity is connected to the logistic network and enables/disables based on logistic_condition. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var connect_to_logistic_network: Boolean = false,
    /** Whether this rail signal can be closed by circuit_condition. */
    public var circuit_close_signal: Boolean? = null,
    /** Whether or not to read the state of this rail/chain signal. */
    public var circuit_read_signal: Boolean? = null,
    /** [SignalID] to use if the rail/chain signal is currently red. */
    public var red_output_signal: SignalID? = null,
    /** [SignalID] to use if the rail/chain signal is currently orange. */
    public var orange_output_signal: SignalID? = null,
    /** [SignalID] to use if the rail/chain signal is currently green. */
    public var green_output_signal: SignalID? = null,
    /** [SignalID] to use if the rail/chain signal is currently blue. */
    public var blue_output_signal: SignalID? = null,
    public var circuit_condition: CircuitCondition? = null,
    /** Enable or disable based on circuit_condition. */
    public var circuit_enable_disable: Boolean? = null,
    /** Send circuit values to the train to use in schedule conditions. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var send_to_train: Boolean = false,
    /** Get the currently stopped trains cargo. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var read_from_train: Boolean = false,
    /** Get the currently stopped trains ID. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var read_stopped_train: Boolean = false,
    /** [SignalID] to output the train ID on. */
    public var train_stopped_signal: SignalID? = null,
    /** Whether this stations train limit will be set through circuit values. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var set_trains_limit: Boolean = false,
    /** [SignalID] to use to set the trains limit. */
    public var trains_limit_signal: SignalID? = null,
    /** Whether to read this stations currently on route trains count. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var read_trains_count: Boolean = false,
    /** [SignalID] to output the on route trains count on. */
    public var trains_count_signal: SignalID? = null,
    /** Whether this roboport should output the contents of its network. */
    public var read_logistics: Boolean? = null,
    /** Whether this roboport should output the robot stats of its network. */
    public var read_robot_stats: Boolean? = null,
    /** [SignalID] to output available logistic robots on. */
    public var available_logistic_output_signal: SignalID? = null,
    /** [SignalID] to output total count of logistic robots on. */
    public var total_logistic_output_signal: SignalID? = null,
    /** [SignalID] to output available construction robots on. */
    public var available_construction_output_signal: SignalID? = null,
    /** [SignalID] to output total count of construction robots on. */
    public var total_construction_output_signal: SignalID? = null,
    /** Whether to limit the gate opening with circuit_condition. */
    public var circuit_open_gate: Boolean? = null,
    /** Whether to send the wall-gate proximity sensor to the circuit network. */
    public var circuit_read_sensor: Boolean? = null,
    /** [SignalID] to output the wall-gate proximity sensor / accumulator charge on. */
    public var output_signal: SignalID? = null,
    /** Whether to read this belts content or inserters hand. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var circuit_read_hand_contents: Boolean = false,
    public var circuit_contents_read_mode: TransportBeltContentReadMode? = null,
    public var circuit_mode_of_operation: CircuitModeOfOperation? = null,
    public var circuit_hand_read_mode: InserterHandReadMode? = null,
    /** Whether to set inserters stack size from a circuit signal. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public var circuit_set_stack_size: Boolean = false,
    /** [SignalID] to use to set the inserters stack size. */
    public var stack_control_input_signal: SignalID? = null,
    /** whether this miner should output its remaining resource amounts to the circuit network. */
    public var circuit_read_resources: Boolean? = null,
    public var circuit_resource_read_mode: MiningDrillResourceReadMode? = null,
    /** Whether this constant combinator is currently on or off. */
    public var is_on: Boolean? = null,
    public var filters: List<ConstantCombinatorParameters>? = null,
    public var arithmetic_conditions: ArithmeticCombinatorParameters? = null,
    public var decider_conditions: DeciderCombinatorParameters? = null,
    public var circuit_parameters: ProgrammableSpeakerCircuitParameters? = null,
    /** Whether this lamp should use colors or not. */
    public var use_colors: Boolean? = null,
)


/**
 * [Online Documentation](https://lua-api.factorio.com/latest/concepts.html#ConstantCombinatorParameters)
 */
@Serializable
public data class ConstantCombinatorParameters(
    /** Signal to emit. */
    public val signal: SignalID,
    /** Value of the signal to emit. */
    public val count: Int,
    /** Index of the constant combinator's slot to set this signal to. */
    public val index: Int,
)

/**
 * [Online Documentation](https://lua-api.factorio.com/latest/concepts.html#ArithmeticCombinatorParameters)
 */
@Serializable
public data class ArithmeticCombinatorParameters(
    /**
     * First signal to use in an operation.
     * If not specified, the second argument will be the value of [first_constant].
     */
    public val first_signal: SignalID? = null,
    /**
     * Second signal to use in an operation.
     * If not specified, the second argument will be the value of [second_constant].
     */
    public val second_signal: SignalID? = null,
    /**
     * Constant to use as the first argument of the operation.
     * Has no effect when [first_signal] is set. Defaults to 0.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val first_constant: Int = 0,
    /**
     * Constant to use as the second argument of the operation.
     * Has no effect when [second_signal] is set. Defaults to 0.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val second_constant: Int = 0,
    /** When not specified, defaults to "*". */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val operation: ArithmeticOperation = ArithmeticOperation.Multiply,
    /** Specifies the signal to output. */
    public val output_signal: SignalID? = null,
)

@Serializable
public enum class ArithmeticOperation {
    @SerialName("*")
    Multiply,

    @SerialName("/")
    Divide,

    @SerialName("+")
    Add,

    @SerialName("-")
    Subtract,

    @SerialName("%")
    Modulo,

    @SerialName("^")
    Power,

    @SerialName("<<")
    ShiftLeft,

    @SerialName(">>")
    ShiftRight,

    @SerialName("AND")
    And,

    @SerialName("OR")
    Or,

    @SerialName("XOR")
    Xor
}

@Serializable
public data class DeciderCombinatorParameters(
    /** Defaults to blank. */
    public val first_signal: SignalID? = null,
    /**
     * Second signal to use in an operation, if any.
     * If this is not specified, the second argument to a decider combinator's operation is assumed to be the value of [constant].
     */
    public val second_signal: SignalID? = null,
    /** Constant to use as the second argument of operation. Defaults to 0. */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val constant: Int = 0,
    /** Specifies how the inputs should be compared. If not specified, defaults to "<". */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val comparator: ComparatorString = ComparatorString.Less,
    /** Defaults to blank. */
    public val output_signal: SignalID? = null,
    /** Defaults to `true`. When `false`, will output a value of `1` for the given [output_signal]. */
    public val copy_count_from_input: Boolean = true,
)

@Serializable
public data class CircuitCondition(
    /** Specifies how the inputs should be compared. If not specified, defaults to "<". */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val comparator: ComparatorString = ComparatorString.Less,
    /** Defaults to blank. */
    public val first_signal: SignalID? = null,
    /** What to compare [first_signal] to. If not specified, [first_signal] will be compared to [constant]. */
    public val second_signal: SignalID? = null,
    /**
     * Constant to compare [first_signal] to. Has no effect when [second_signal] is set.
     * When neither [second_signal] nor [constant] are specified, the effect is as though constant were specified with the value 0.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    public val constant: Int = 0,
)

@Serializable
public enum class ComparatorString {
    @SerialName("=")
    Equal,

    @SerialName(">")
    Greater,

    @SerialName("<")
    Less,

    @SerialName("≥")
    GreaterOrEqual,

    @SerialName("≤")
    LessOrEqual,

    @SerialName("≠")
    NotEqual,
}

private fun ImportableBlueprint.toStringImpl(name: String) = buildString {
    append(name)
    append('(')
    if (label != null) {
        append('"').append(label).append("\" ")
    }
    if (this@toStringImpl is Blueprint) {
        if (!entities.isNullOrEmpty() || tiles.isNullOrEmpty()) {
            append("with ").append(entities!!.size).append(" entities")
        } else {
            append("with ").append(tiles!!.size).append(" tiles")
        }
    } else {
        this@toStringImpl as BlueprintBook
        append("with ").append(blueprints.size).append(" items")
    }
    val description = description
    if (!description.isNullOrEmpty()) {
        append(", \"")
        append(description.take(20))
        if (description.length > 20) {
            append("...")
        }
        append("\"")
    }
    append(')')
}
