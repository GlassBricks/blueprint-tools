package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.AlertParameters
import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ProgrammableSpeakerCircuitParameters
import glassbricks.factorio.blueprint.json.SpeakerParameters
import glassbricks.factorio.blueprint.prototypes.ProgrammableSpeakerPrototype

public class ProgrammableSpeaker(
    override val prototype: ProgrammableSpeakerPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)

    public var allowPolyphony: Boolean = json.parameters?.allow_polyphony ?: false
    public var playbackGlobally: Boolean = json.parameters?.playback_globally ?: false
    public var playbackVolume: Double = json.parameters?.playback_volume ?: 1.0

    public var alertMessage: String = json.alert_parameters?.alert_message ?: ""
    public var showAlert: Boolean = json.alert_parameters?.show_alert ?: false
    public var showOnMap: Boolean = json.alert_parameters?.show_on_map ?: true

    public override val controlBehavior: ProgrammableSpeakerControlBehavior =
        ProgrammableSpeakerControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.parameters = SpeakerParameters(
            allow_polyphony = allowPolyphony,
            playback_globally = playbackGlobally,
            playback_volume = playbackVolume
        )
        json.alert_parameters = AlertParameters(
            alert_message = alertMessage,
            show_alert = showAlert,
            show_on_map = showOnMap
        )
        if (this.hasCircuitConnections())
            json.control_behavior = controlBehavior.exportToJson()
    }
}

public class ProgrammableSpeakerControlBehavior(json: ControlBehaviorJson?) : ControlBehavior {
    public var circuitCondition: CircuitCondition = json?.circuit_condition ?: CircuitCondition.DEFAULT
    public var signalValueIsPitch: Boolean = json?.circuit_parameters?.signal_value_is_pitch ?: false
    public var instrumentId: Int = json?.circuit_parameters?.instrument_id ?: 0
    public var noteId: Int = json?.circuit_parameters?.note_id ?: 0

    override fun exportToJson(): ControlBehaviorJson = ControlBehaviorJson(
        circuit_condition = circuitCondition,
        circuit_parameters = ProgrammableSpeakerCircuitParameters(
            signal_value_is_pitch = signalValueIsPitch,
            instrument_id = instrumentId,
            note_id = noteId
        )
    )
}
