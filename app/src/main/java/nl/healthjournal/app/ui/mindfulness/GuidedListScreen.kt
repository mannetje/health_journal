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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.ContentLanguage
import nl.healthjournal.domain.model.mindfulness.GuidedExercise
import nl.healthjournal.domain.model.mindfulness.GuidedExerciseCatalogue

/** The recorded exercises, the ones in the app language first. Opening one does not record anything. */
@Composable
fun GuidedListScreen(onOpen: (GuidedExercise) -> Unit) {
    val appLanguage = appContentLanguage()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GuidedExerciseCatalogue.orderedFor(appLanguage).forEach { exercise ->
            Card(onClick = { onOpen(exercise) }, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(exercise.title.forLanguage(appLanguage), style = MaterialTheme.typography.titleMedium)
                    Text(exercise.description.forLanguage(appLanguage), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        listOfNotNull(
                            clockText(exercise.durationSeconds),
                            if (exercise.audioMatches(appLanguage)) null
                            else stringResource(R.string.guided_audio_language, languageName(exercise.audioLanguage))
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
internal fun appContentLanguage(): ContentLanguage = ContentLanguage.fromTag(LocalConfiguration.current.locales[0].language)

@Composable
internal fun languageName(language: ContentLanguage): String = stringResource(
    when (language) {
        ContentLanguage.ENGLISH -> R.string.guided_language_en
        ContentLanguage.DUTCH -> R.string.guided_language_nl
    }
)

@Composable
internal fun clockText(totalSeconds: Int): String =
    stringResource(R.string.guided_time, totalSeconds / 60, totalSeconds % 60)
