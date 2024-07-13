package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.Color
import glassbricks.factorio.blueprint.json.SignalID
import glassbricks.factorio.blueprint.prototypes.TrainStopPrototype


public class TrainStop(
    override val prototype: TrainStopPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectable, WithColor {
    public var station: String? = json.station
    public override var color: Color? = json.color
    public var manualTrainsLimit: Int? = json.manual_trains_limit

    override val connectionPoint1: CircuitConnectionPoint = CircuitConnectionPoint(this)
    public override val controlBehavior: TrainStopControlBehavior = TrainStopControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.station = station
        json.color = color
        json.manual_trains_limit = manualTrainsLimit
        if (this.hasCircuitConnections()) json.control_behavior = controlBehavior.exportToJson()
    }

}

public class TrainStopControlBehavior(json: ControlBehaviorJson?) : GenericOnOffControlBehavior(json), ControlBehavior {
    public var sendToTrain: Boolean = json?.send_to_train ?: false
    public var readFromTrain: Boolean = json?.read_from_train ?: false

    public var trainStoppedSignal: SignalID? = json?.train_stopped_signal
        ?.takeIf { json.read_stopped_train }
    public var trainsLimitSignal: SignalID? = json?.trains_limit_signal
        ?.takeIf { json.set_trains_limit }
    public var trainsCountSignal: SignalID? = json?.trains_count_signal
        ?.takeIf { json.read_trains_count }

    override fun exportToJson(): ControlBehaviorJson = super.baseExportToJson().apply {
        circuit_enable_disable = if (circuitCondition != null) true else null
        send_to_train = sendToTrain
        read_from_train = readFromTrain
        read_stopped_train = trainStoppedSignal != null
        train_stopped_signal = trainStoppedSignal
        set_trains_limit = trainsLimitSignal != null
        trains_limit_signal = trainsLimitSignal
        read_trains_count = trainsCountSignal != null
        trains_count_signal = trainsCountSignal
    }
}
