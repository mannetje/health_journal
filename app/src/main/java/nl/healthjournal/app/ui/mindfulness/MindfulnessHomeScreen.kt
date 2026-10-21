package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R

/**
 * The home of the Mindfulness area: the four ways in and the link to the full text about the exercises.
 * Nothing here records what the user did; the screen only opens other screens.
 */
@Composable
fun MindfulnessHomeScreen(
    onBreathe: () -> Unit = {},
    onMeditate: () -> Unit = {},
    onGuided: () -> Unit = {},
    onStartHere: () -> Unit = {},
    onAbout: () -> Unit = {},
    onCredits: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeCard("🌬️", R.string.mindfulness_home_breathe, R.string.mindfulness_home_breathe_desc, onBreathe)
        HomeCard("🔔", R.string.mindfulness_home_meditate, R.string.mindfulness_home_meditate_desc, onMeditate)
        HomeCard("🎧", R.string.mindfulness_home_guided, R.string.mindfulness_home_guided_desc, onGuided)
        HomeCard("🌱", R.string.mindfulness_home_start_here, R.string.mindfulness_home_start_here_desc, onStartHere)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAbout) { Text(stringResource(R.string.mindfulness_home_about)) }
            TextButton(onClick = onCredits) { Text(stringResource(R.string.mindfulness_home_credits)) }
        }
    }
}

@Composable
private fun HomeCard(emoji: String, title: Int, description: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
