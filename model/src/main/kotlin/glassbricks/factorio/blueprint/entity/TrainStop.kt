package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.TrainStopPrototype


public class TrainStop(
    override val prototype: TrainStopPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior, WithColor {
    public var station: String? = json.station
    public override var color: Color? = json.color
    public var manualTrainsLimit: Int? = json.manual_trains_limit

    override val circuitConnections: CircuitConnections = CircuitConnections(this)
    public override val controlBehavior: TrainStopControlBehavior =
        TrainStopControlBehavior(prototype, json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.station = station
        json.color = color
        json.manual_trains_limit = manualTrainsLimit
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): TrainStop = TrainStop(prototype, jsonForCopy())
}

public class TrainStopControlBehavior(
    private val prototype: TrainStopPrototype,
    json: ControlBehaviorJson?
) : GenericOnOffControlBehavior(json), ControlBehavior {
    public var sendToTrain: Boolean = json?.send_to_train ?: false
    public var readFromTrain: Boolean = json?.read_from_train ?: false

    public val defaultTrainStoppedSignal: SignalID? get() = prototype.default_train_stopped_signal
    public val defaultTrainsLimitSignal: SignalID? get() = prototype.default_trains_limit_signal
    public val defaultTrainsCountSignal: SignalID? get() = prototype.default_trains_count_signal

    public var trainStoppedSignal: SignalID? = json?.train_stopped_signal
        ?.toSignalIDBasic()
        ?.takeIf { json.read_stopped_train }
    public var trainsLimitSignal: SignalID? = json?.trains_limit_signal
        ?.toSignalIDBasic()
        ?.takeIf { json.set_trains_limit }
    public var trainsCountSignal: SignalID? = json?.trains_count_signal
        ?.toSignalIDBasic()
        ?.takeIf { json.read_trains_count }

    override fun exportToJson(): ControlBehaviorJson =
        super.baseExportToJson().apply {
            circuit_enable_disable = if (circuitCondition != null) true else null
            send_to_train = sendToTrain
            read_from_train = readFromTrain
            read_stopped_train = trainStoppedSignal != null
            // these don't actually use default...
            // instead, in game, it gets set to the default when a new control behavior is created
            train_stopped_signal = trainStoppedSignal?.toJsonBasic()
            set_trains_limit = trainsLimitSignal != null
            trains_limit_signal = trainsLimitSignal?.toJsonBasic()
            read_trains_count = trainsCountSignal != null
            trains_count_signal = trainsCountSignal?.toJsonBasic()
        }
}
