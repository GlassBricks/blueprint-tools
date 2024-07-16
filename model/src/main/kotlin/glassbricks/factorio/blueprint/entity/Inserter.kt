package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.InserterPrototype

public class Inserter(override val prototype: InserterPrototype, json: EntityJson) :
    BaseEntity(json), WithItemFilters, CircuitConnectionPoint, WithControlBehavior {
    override val filters: Array<String?> = json.filters.toFilterArray(prototype.filter_count.toInt())
    public var filterMode: FilterMode = json.filter_mode
    public var overrideStackSize: Byte? = json.override_stack_size?.toByte()
    public var dropPosition: Position? = json.drop_position
    public var pickupPosition: Position? = json.pickup_position

    public override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: InserterControlBehavior =
        InserterControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.filters = filtersAsIndexList()
        json.filter_mode = filterMode
        json.override_stack_size = overrideStackSize?.toUByte()
        json.drop_position = dropPosition
        json.pickup_position = pickupPosition
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): Inserter = Inserter(prototype, toDummyJson())
}

public class InserterControlBehavior(json: ControlBehaviorJson?) : GenericOnOffControlBehavior(json), ControlBehavior {
    public var modeOfOperation: InserterModeOfOperation =
        json?.circuit_mode_of_operation?.asInserter() ?: InserterModeOfOperation.EnableDisable
    public var readContentsMode: InserterHandReadMode? =
        if (json?.circuit_read_hand_contents == true)
            json.circuit_hand_read_mode ?: InserterHandReadMode.Pulse else null

    override fun shouldHaveCircuitCondition(source: ControlBehaviorJson): Boolean =
        source.circuit_mode_of_operation.let {
            it == null || it.rawValue == InserterModeOfOperation.EnableDisable.ordinal
        }

    public var setStackSizeSignal: SignalID? =
        json?.stack_control_input_signal?.toSignalIDBasic()
            ?.takeIf { json.circuit_set_stack_size }

    override fun exportToJson(): ControlBehaviorJson? {
        val modeOfOperation = modeOfOperation.asMode().takeUnless { it.rawValue == 0 }
        val circuitCondition = circuitCondition
            .takeIf { this.modeOfOperation == InserterModeOfOperation.EnableDisable }
            ?.takeUnless { it == CircuitCondition.DEFAULT }
        val connectToNetwork = logisticCondition != null
        val readContents = readContentsMode != null
        val setStackSize = setStackSizeSignal != null
        if (modeOfOperation == null
            && circuitCondition == null
            && !connectToNetwork
            && !readContents
            && !setStackSize
        ) return null

        return ControlBehaviorJson(
            circuit_mode_of_operation = modeOfOperation,
            circuit_condition = circuitCondition,

            connect_to_logistic_network = connectToNetwork,
            logistic_condition = logisticCondition,

            circuit_read_hand_contents = readContents,
            circuit_hand_read_mode = readContentsMode.takeIf { it == InserterHandReadMode.Hold },

            circuit_set_stack_size = setStackSize,
            stack_control_input_signal = setStackSizeSignal
                .takeIf { setStackSize }
                ?.toJsonBasic()
        )
    }
}
