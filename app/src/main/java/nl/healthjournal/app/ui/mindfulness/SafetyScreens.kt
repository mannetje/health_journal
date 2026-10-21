package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R

/**
 * The note shown the first time the area is opened: the text, one button to acknowledge it and a quiet link to
 * the full text, and nothing else.
 */
@Composable
fun SafetyNoteScreen(onAcknowledge: () -> Unit, onReadMore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.safety_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.safety_text), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.safety_acknowledge))
        }
        TextButton(onClick = onReadMore) { Text(stringResource(R.string.safety_read_more)) }
    }
}

/** The full text about the exercises, reached from the note and from the home screen. */
@Composable
fun AboutExercisesScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.mindfulness_home_about), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.safety_about_text), style = MaterialTheme.typography.bodyLarge)
    }
}
