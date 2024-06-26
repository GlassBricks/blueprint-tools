package glassbricks.factorio.blueprint

import kotlinx.serialization.Serializable


/**
 * The [ordinal] represents the encoded direction.
 */
@Serializable(with = Direction.Serializer::class)
public enum class Direction {
    North,
    Northeast,
    East,
    Southeast,
    South,
    Southwest,
    West,
    Northwest;

    public companion object {
        public fun fromByte(byte: Byte): Direction = entries[byte.toInt()]
    }

    public fun toByte(): Byte = ordinal.toByte()

    public fun oppositeDirection(): Direction = entries[(ordinal + 4) % 8]
    public val isCardinal: Boolean get() = ordinal % 2 == 0

    internal object Serializer : EnumOrdinalSerializer<Direction>(Direction::class)
}

@Serializable(with = TransportBeltContentReadMode.Serializer::class)
public enum class TransportBeltContentReadMode {
    Pulse, Hold;

    internal object Serializer :
        EnumOrdinalSerializer<TransportBeltContentReadMode>(TransportBeltContentReadMode::class)
}

@Serializable
@JvmInline
public value class CircuitModeOfOperation(public val rawValue: Int) {
    init {
        require(rawValue in 0..4) { "CircuitModeOfOperation must be in range 0..4" }
    }

    public fun asInserter(): InserterCircuitMode = InserterCircuitMode.entries[rawValue]
    public fun asLamp(): LampCircuitMode = LampCircuitMode.entries[rawValue]
    public fun asLogisticContainer(): LogisticContainerCircuitMode =
        LogisticContainerCircuitMode.entries[rawValue]

    public fun from(value: CircuitModeOption): CircuitModeOfOperation = CircuitModeOfOperation(value.ordinal)
    public fun equalsOption(value: CircuitModeOption): Boolean = rawValue == value.ordinal
}

public sealed interface CircuitModeOption {
    public val ordinal: Int
}

public fun CircuitModeOption.mode(): CircuitModeOfOperation = CircuitModeOfOperation(ordinal)

public enum class InserterCircuitMode : CircuitModeOption {
    EnableDisable,
    SetFilters,
    ReadHandContents,
    None,
    SetStackSize
}

public enum class LampCircuitMode : CircuitModeOption {
    UseColors
}

public enum class LogisticContainerCircuitMode : CircuitModeOption {
    SendContents,
    SetRequests
}


@Serializable(with = InserterHandReadMode.Serializer::class)
public enum class InserterHandReadMode {
    Pulse, Hold;

    internal object Serializer : EnumOrdinalSerializer<InserterHandReadMode>(InserterHandReadMode::class)
}

@Serializable(with = MiningDrillResourceReadMode.Serializer::class)
public enum class MiningDrillResourceReadMode {
    ThisMiner, EntirePatch;

    internal object Serializer :
        EnumOrdinalSerializer<MiningDrillResourceReadMode>(MiningDrillResourceReadMode::class)
}
