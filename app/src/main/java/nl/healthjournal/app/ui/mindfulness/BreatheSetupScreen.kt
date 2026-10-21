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
import nl.healthjournal.domain.model.mindfulness.BreathingDuration
import nl.healthjournal.domain.model.mindfulness.BreathingPattern
import nl.healthjournal.domain.model.mindfulness.BreathingPatternId
import nl.healthjournal.domain.model.mindfulness.BreathingPatterns
import nl.healthjournal.domain.model.mindfulness.CueChoices

/** Choices for a breathing exercise: pattern, length and how it is drawn. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreatheSetupScreen(
    patternId: BreathingPatternId,
    duration: BreathingDuration,
    visual: BreathingVisual,
    onPattern: (BreathingPatternId) -> Unit,
    onDuration: (BreathingDuration) -> Unit,
    onVisual: (BreathingVisual) -> Unit,
    cues: CueChoices,
    onCues: (CueChoices) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.breathing_pattern_heading), style = MaterialTheme.typography.titleMedium)
        BreathingPatterns.builtIn.forEach { pattern ->
            FilterChip(
                selected = pattern.id == patternId,
                onClick = { onPattern(pattern.id) },
                label = { Text(patternName(pattern)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(stringResource(R.string.breathing_length_heading), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreathingDuration.entries.forEach { d ->
                FilterChip(
                    selected = d == duration,
                    onClick = { onDuration(d) },
                    label = { Text(stringResource(R.string.breathing_minutes, d.seconds / 60)) }
                )
            }
        }
        Text(stringResource(R.string.breathing_visual_heading), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreathingVisual.entries.forEach { v ->
                FilterChip(
                    selected = v == visual,
                    onClick = { onVisual(v) },
                    label = { Text(stringResource(visualLabel(v))) }
                )
            }
        }
        CueToggles(cues, onCues, R.string.cues_tones_breathing, R.string.cues_haptics_breathing, showSpeech = true)
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.breathing_start))
        }
    }
}

@Composable
private fun patternName(pattern: BreathingPattern): String {
    val name = stringResource(
        when (pattern.id) {
            BreathingPatternId.BOX -> R.string.breathing_pattern_box
            BreathingPatternId.FOUR_SEVEN_EIGHT -> R.string.breathing_pattern_478
            BreathingPatternId.PHYSIOLOGICAL_SIGH -> R.string.breathing_pattern_sigh
            BreathingPatternId.BELLY -> R.string.breathing_pattern_belly
            BreathingPatternId.CUSTOM -> R.string.breathing_pattern_custom
        }
    )
    return name + " (" + pattern.phases.joinToString("-") { it.seconds.toString() } + ")"
}

private fun visualLabel(visual: BreathingVisual): Int = when (visual) {
    BreathingVisual.LOTUS -> R.string.breathing_visual_lotus
    BreathingVisual.CIRCLE -> R.string.breathing_visual_circle
    BreathingVisual.SQUARE -> R.string.breathing_visual_square
}
