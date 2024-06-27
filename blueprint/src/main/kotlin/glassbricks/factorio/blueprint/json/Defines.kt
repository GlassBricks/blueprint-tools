package glassbricks.factorio.blueprint.json

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

    public fun asInserter(): InserterModeOfOperation = InserterModeOfOperation.entries[rawValue]
    public fun asLamp(): LampModeOfOperation = LampModeOfOperation.entries[rawValue]
    public fun asLogisticContainer(): LogisticContainerModeOfOperation =
        LogisticContainerModeOfOperation.entries[rawValue]

    public fun from(value: CircuitModeOption): CircuitModeOfOperation = CircuitModeOfOperation(value.ordinal)
    public fun equalsOption(value: CircuitModeOption): Boolean = rawValue == value.ordinal
}

public sealed interface CircuitModeOption {
    public val ordinal: Int
}

public fun CircuitModeOption.asMode(): CircuitModeOfOperation = CircuitModeOfOperation(ordinal)

public enum class InserterModeOfOperation : CircuitModeOption {
    EnableDisable,
    SetFilters,
    /** This is not actually used. */
    ReadHandContents, 
    None,
    /** This is not actually used. */
    SetStackSize 
}

public enum class LampModeOfOperation : CircuitModeOption {
    UseColors
}

public enum class LogisticContainerModeOfOperation : CircuitModeOption {
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
