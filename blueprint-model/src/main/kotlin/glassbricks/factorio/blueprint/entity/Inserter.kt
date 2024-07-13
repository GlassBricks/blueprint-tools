package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.Position
import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.InserterPrototype

public class Inserter(override val prototype: InserterPrototype, json: EntityJson) :
    BaseEntity(json), WithItemFilters, CircuitConnectionPoint, WithControlBehavior {
    override val filters: Array<String?> = json.filters.toFilters(prototype.filter_count?.toInt() ?: 0)
    public var filterMode: FilterMode = json.filter_mode
    public var overrideStackSize: Byte? = json.override_stack_size?.toByte()
    public var dropPosition: Position? = json.drop_position
    public var pickupPosition: Position? = json.pickup_position

    public override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: InserterControlBehavior = InserterControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.filters = getFiltersAsList()
        json.filter_mode = filterMode
        json.override_stack_size = overrideStackSize?.toUByte()
        json.drop_position = dropPosition
        json.pickup_position = pickupPosition
        if(this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }
}

public class InserterControlBehavior(
    json: ControlBehaviorJson?,
) : GenericOnOffControlBehavior(json), ControlBehavior {
    public var modeOfOperation: InserterModeOfOperation =
        json?.circuit_mode_of_operation?.asInserter() ?: InserterModeOfOperation.None
    public var readContentsMode: InserterHandReadMode? = json?.circuit_hand_read_mode
    public var setStackSizeSignal: SignalID? = json?.stack_control_input_signal?.toSignalIDBasic()?.takeIf { json.circuit_set_stack_size }

    override fun exportToJson(): ControlBehaviorJson = ControlBehaviorJson(
        circuit_mode_of_operation = modeOfOperation.asMode(),
        circuit_condition = circuitCondition.takeIf {
            modeOfOperation == InserterModeOfOperation.EnableDisable
        },
        logistic_condition = logisticCondition,
        connect_to_logistic_network = logisticCondition != null,

        circuit_read_hand_contents = readContentsMode != null,
        circuit_hand_read_mode = readContentsMode,

        circuit_set_stack_size = setStackSizeSignal != null,
        stack_control_input_signal = setStackSizeSignal.toJsonBasic(),
    )
}
