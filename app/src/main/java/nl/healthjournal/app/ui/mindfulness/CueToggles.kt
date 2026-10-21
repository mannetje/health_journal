package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.CueChoices

/**
 * The optional cues of an exercise: sound, vibration and, when [showSpeech] is set, spoken words. Spoken cues
 * need a voice that works offline; without one the switch is off and says so, and the exercise uses text only.
 */
@Composable
fun CueToggles(
    choices: CueChoices,
    onChange: (CueChoices) -> Unit,
    tonesLabel: Int,
    hapticsLabel: Int,
    showSpeech: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.cues_heading), style = MaterialTheme.typography.titleMedium)
        CueRow(stringResource(tonesLabel), choices.tones, true) { onChange(choices.copy(tones = it)) }
        CueRow(stringResource(hapticsLabel), choices.haptics, true) { onChange(choices.copy(haptics = it)) }
        if (showSpeech) {
            val offline = rememberOfflineVoices(appContentLanguage())
            val voiceOk = offline?.voices?.isNotEmpty() == true
            CueRow(stringResource(R.string.cues_speech), choices.speech && voiceOk, voiceOk) {
                onChange(choices.copy(speech = it))
            }
            if (offline != null && !voiceOk) {
                Text(stringResource(R.string.cues_speech_unavailable), style = MaterialTheme.typography.bodySmall)
            }
            if (choices.speech && offline != null && offline.voices.size > 1) {
                VoicePicker(offline, choices.voiceName ?: offline.voices.first().name) {
                    onChange(choices.copy(voiceName = it))
                }
            }
        }
    }
}

@Composable
private fun CueRow(label: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChecked
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun VoicePicker(offline: OfflineVoices, selected: String, onSelect: (String) -> Unit) {
    val sample = stringResource(R.string.breathing_phase_inhale)
    Text(stringResource(R.string.cues_voice_heading), style = MaterialTheme.typography.titleSmall)
    val mixedLocales = offline.voices.map { it.locale }.toSet().size > 1
    offline.voices.forEachIndexed { index, voice ->
        Row(
            modifier = Modifier.fillMaxWidth().selectable(
                selected = voice.name == selected, role = Role.RadioButton, onClick = { onSelect(voice.name) }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = voice.name == selected, onClick = null)
            Text(
                stringResource(R.string.cues_voice_item, index + 1) + if (mixedLocales) " (" + voice.locale.displayName + ")" else "",
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(onClick = { offline.preview(voice.name, sample) }) {
                Text(stringResource(R.string.cues_voice_preview))
            }
        }
    }
}
