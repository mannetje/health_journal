package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.CueChoices
import nl.healthjournal.domain.model.mindfulness.MeditationTimer

/** Choices for a silent sitting: how long, and whether a soft bell sounds on the way. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeditateSetupScreen(
    minutes: Int,
    intervalMinutes: Int?,
    onMinutes: (Int) -> Unit,
    onInterval: (Int?) -> Unit,
    cues: CueChoices,
    onCues: (CueChoices) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.breathing_length_heading), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeditationTimer.PRESET_MINUTES.forEach { m ->
                FilterChip(
                    selected = m == minutes,
                    onClick = { onMinutes(m) },
                    label = { Text(stringResource(R.string.breathing_minutes, m)) }
                )
            }
        }
        Text(stringResource(R.string.meditate_interval_heading), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = intervalMinutes == null,
                onClick = { onInterval(null) },
                label = { Text(stringResource(R.string.meditate_interval_none)) }
            )
            MeditationTimer.INTERVAL_PRESET_MINUTES.filter { it < minutes }.forEach { m ->
                FilterChip(
                    selected = m == intervalMinutes,
                    onClick = { onInterval(m) },
                    label = { Text(stringResource(R.string.meditate_interval_every, m)) }
                )
            }
        }
        CueToggles(cues, onCues, R.string.cues_bells, R.string.cues_haptics_bells, showSpeech = false)
        if (!cues.tones && !cues.haptics) {
            Text(stringResource(R.string.meditate_silent_hint), style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.breathing_start))
        }
    }
}
