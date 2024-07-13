package glassbricks.factorio.blueprint.entity

import glassbricks.factorio.blueprint.json.AlertParameters
import glassbricks.factorio.blueprint.json.CircuitCondition
import glassbricks.factorio.blueprint.json.ProgrammableSpeakerCircuitParameters
import glassbricks.factorio.blueprint.json.SpeakerParameters
import org.junit.jupiter.api.Test

class ProgrammableSpeakerTest {
    @Test
    fun `can load programmable speaker`() {
        testSaveLoad(ProgrammableSpeaker::class, "programmable-speaker",
            connectToNetwork = true,
            build = fun EntityJson.() {
                control_behavior = ControlBehaviorJson(
                    circuit_condition = CircuitCondition(),
                    circuit_parameters = ProgrammableSpeakerCircuitParameters(
                        signal_value_is_pitch = true,
                        instrument_id = 1,
                        note_id = 2
                    )
                )
                parameters = SpeakerParameters(
                    allow_polyphony = true,
                    playback_globally = true,
                    playback_volume = 0.5
                )
                alert_parameters = AlertParameters(
                    alert_message = "Hello, world!",
                    show_alert = true,
                    show_on_map = false
                )
            })
    }
}
