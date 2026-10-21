package nl.healthjournal.app.ui.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.GuidedExerciseCatalogue

/**
 * Where the outside material of the area comes from: each recording with its teacher, source and licence, and links
 * to the original and to the licence text. The links open in the browser of the device; the app itself has no network
 * access.
 */
@Composable
fun CreditsScreen() {
    val appLanguage = appContentLanguage()
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.mindfulness_home_credits), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.credits_intro), style = MaterialTheme.typography.bodyLarge)
        GuidedExerciseCatalogue.exercises.forEach { exercise ->
            val credit = exercise.credit
            Column {
                Text(exercise.title.forLanguage(appLanguage), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.credits_item, credit.author, credit.sourceName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    stringResource(R.string.credits_licence, credit.licenceName),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { uriHandler.openUri(credit.sourceUrl) }) {
                        Text(stringResource(R.string.credits_open_original))
                    }
                    TextButton(onClick = { uriHandler.openUri(credit.licenceUrl) }) {
                        Text(stringResource(R.string.credits_open_licence))
                    }
                }
            }
        }
        Text(stringResource(R.string.credits_own), style = MaterialTheme.typography.bodyMedium)
    }
}
