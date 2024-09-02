package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.SignalID
import glassbricks.factorio.blueprint.json.*
import glassbricks.factorio.blueprint.prototypes.ProgrammableSpeakerPrototype

public class ProgrammableSpeaker(
    override val prototype: ProgrammableSpeakerPrototype,
    json: EntityJson,
) : BaseEntity(json), CircuitConnectionPoint, WithControlBehavior {
    override val circuitConnections: CircuitConnections = CircuitConnections(this)

    public var allowPolyphony: Boolean = json.parameters?.allow_polyphony ?: false
    public var playbackGlobally: Boolean = json.parameters?.playback_globally ?: false
    public var playbackVolume: Double = json.parameters?.playback_volume ?: 1.0

    public var showAlert: Boolean = json.alert_parameters?.show_alert ?: false
    public var showOnMap: Boolean = json.alert_parameters?.show_on_map ?: true
    public var alertMessage: String = json.alert_parameters?.alert_message ?: ""
    public var icon_signal_id: SignalID? = json.alert_parameters?.icon_signal_id?.toSignalIDBasic()

    public override val controlBehavior: ProgrammableSpeakerControlBehavior =
        ProgrammableSpeakerControlBehavior(json.control_behavior)

    override fun exportToJson(json: EntityJson) {
        json.parameters = SpeakerParameters(
            allow_polyphony = allowPolyphony,
            playback_globally = playbackGlobally,
            playback_volume = playbackVolume
        )
        json.alert_parameters = AlertParameters(
            show_alert = showAlert,
            show_on_map = showOnMap,
            icon_signal_id = icon_signal_id?.toJsonBasic(),
            alert_message = alertMessage,
        )
        if (this.shouldExportControlBehavior()) json.control_behavior = controlBehavior.exportToJson()
    }

    override fun copyIsolated(): ProgrammableSpeaker = ProgrammableSpeaker(prototype, jsonForCopy())
}

public class ProgrammableSpeakerControlBehavior(json: ControlBehaviorJson?) :
    ControlBehavior {
    public var circuitCondition: CircuitCondition = json?.circuit_condition ?: CircuitCondition.DEFAULT
    public var signalValueIsPitch: Boolean = json?.circuit_parameters?.signal_value_is_pitch ?: false
    public var instrumentId: Int = json?.circuit_parameters?.instrument_id ?: 0
    public var noteId: Int = json?.circuit_parameters?.note_id ?: 0

    override fun exportToJson(): ControlBehaviorJson =
        ControlBehaviorJson(
            circuit_condition = circuitCondition.takeUnless { it == CircuitCondition.DEFAULT },
            circuit_parameters = ProgrammableSpeakerCircuitParameters(
                signal_value_is_pitch = signalValueIsPitch,
                instrument_id = instrumentId,
                note_id = noteId
            )
        )
}
