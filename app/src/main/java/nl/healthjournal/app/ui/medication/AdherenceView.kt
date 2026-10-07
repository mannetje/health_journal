package nl.healthjournal.app.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.medication.Adherence
import nl.healthjournal.domain.model.medication.AdherenceCounts
import nl.healthjournal.domain.model.medication.MissedIntake
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val MISSED_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/**
 * Taken against planned over the chosen range, as plain figures. No colour or wording judges the result:
 * the bars use one neutral colour and the text only states counts.
 */
@Composable
internal fun AdherenceView(state: MedicationUiState, onRange: (Int) -> Unit) {
    val report = state.adherence

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { RangeChooser(state.adherenceDays, onRange) }
        if (report == null || report.overall == null) {
            item { Text(stringResource(R.string.adherence_empty), style = MaterialTheme.typography.bodyLarge) }
        } else {
            item { CountsCard(stringResource(R.string.adherence_overall), report.overall!!) }
            items(report.rows, key = { "adherence-${it.medication.id}" }) { row ->
                CountsCard(row.medication.name.value, row.counts)
            }
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.adherence_streak_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            pluralStringResource(R.plurals.adherence_streak_days, report.streak, report.streak),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            if (report.missed.isNotEmpty()) {
                item { Text(stringResource(R.string.adherence_missed_title), style = MaterialTheme.typography.titleMedium) }
                items(report.missed, key = { "missed-${it.medication.id}-${it.planned}" }) { MissedRow(it) }
            }
        }
        item { Text(stringResource(R.string.adherence_note), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun RangeChooser(selected: Int, onSelect: (Int) -> Unit) {
    val description = stringResource(R.string.adherence_range_label)
    val labels = Adherence.RANGES_IN_DAYS.map { pluralStringResource(R.plurals.adherence_range_days, it, it) }
    ChoiceRow(
        labels = labels,
        selected = Adherence.RANGES_IN_DAYS.indexOf(selected),
        onSelect = { onSelect(Adherence.RANGES_IN_DAYS[it]) },
        stackFromFontScale = 1.5f,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = description }
    )
}

@Composable
private fun CountsCard(title: String, counts: AdherenceCounts) {
    val percentage = counts.percentage
    val takenOfDue = if (counts.due > 0) stringResource(R.string.adherence_taken_of_due, counts.taken, counts.due) else null
    val doses = if (counts.asNeededDoses > 0) {
        pluralStringResource(R.plurals.adherence_doses_logged, counts.asNeededDoses, counts.asNeededDoses)
    } else {
        null
    }
    val details = listOfNotNull(
        takenOfDue,
        doses,
        stringResource(R.string.adherence_skipped_count, counts.skipped).takeIf { counts.due > 0 },
        stringResource(R.string.adherence_missed_count, counts.missed).takeIf { counts.due > 0 }
    )
    val percentText = percentage?.let { stringResource(R.string.adherence_percentage, it) }
    Card(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = (listOf(title) + listOfNotNull(percentText) + details).joinToString(", ")
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (percentage != null && percentText != null) {
                Text(percentText, style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.fillMaxWidth())
            }
            details.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun MissedRow(item: MissedIntake) {
    Text(
        "${item.medication.name.value} · ${item.planned.format(MISSED_FORMAT)}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    )
}
