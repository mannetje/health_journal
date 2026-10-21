package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.BreathingPatterns
import nl.healthjournal.domain.model.mindfulness.StartHere
import nl.healthjournal.domain.model.mindfulness.StartHereStep

/**
 * The short ladder for a first time: the steps in order, each opening its exercise with the settings filled in.
 * Nothing records which step was taken.
 */
@Composable
fun StartHereScreen(onStep: (StartHereStep) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.mindfulness_home_start_here), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.start_here_intro), style = MaterialTheme.typography.bodyLarge)
        StartHere.steps.forEachIndexed { index, step ->
            val label = when (step) {
                is StartHereStep.Breathing -> stringResource(
                    R.string.start_here_step,
                    index + 1,
                    patternName(BreathingPatterns.byId(step.patternId) ?: BreathingPatterns.belly),
                    step.duration.seconds / 60
                )
                is StartHereStep.Timer -> stringResource(
                    R.string.start_here_step,
                    index + 1,
                    stringResource(R.string.start_here_timer_name),
                    step.durationMinutes
                )
            }
            Card(onClick = { onStep(step) }, modifier = Modifier.fillMaxWidth()) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
