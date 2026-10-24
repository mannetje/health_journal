package nl.healthjournal.app.ui.history

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.formatMinutes
import nl.healthjournal.app.ui.migraine.areaLabel
import nl.healthjournal.app.ui.migraine.painLabel
import nl.healthjournal.app.ui.migraine.reliefLabel
import nl.healthjournal.app.ui.migraine.symptomLabel
import nl.healthjournal.app.ui.migraine.triggerLabel
import nl.healthjournal.app.ui.migraine.typeLabel
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineTag

/**
 * One migraine in History, in the same card as the other entry types. Everything that was filled
 * in is shown as short "Label: values" lines; empty parts are left out. Plain facts only: the
 * intensity, the 2-hour score and the "helped me" ticks are the person's own notes and are never
 * interpreted or counted.
 */
@Composable
fun MigraineHistoryCard(
    entry: MigraineEntry,
    tags: List<MigraineTag>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnd: () -> Unit
) {
    EntryCard(onEdit = onEdit, onDelete = onDelete) { MigraineCardContent(entry, tags, onEnd) }
}

@Composable
private fun ColumnScope.MigraineCardContent(entry: MigraineEntry, tags: List<MigraineTag>, onEnd: () -> Unit) {
    val title = entry.intensity
        ?.let { stringResource(R.string.history_migraine_line_intensity, it) }
        ?: stringResource(R.string.migraine_card_title)
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.history_start_line, entry.start.formatLocal()),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (entry.wokeUpWithIt) Text(stringResource(R.string.history_migraine_during_sleep))
    entry.duration
        ?.let { Text(stringResource(R.string.history_migraine_duration, formatMinutes(it.seconds))) }
        ?: Text(stringResource(R.string.history_migraine_ongoing))

    val labelOf = tags.associate { it.id to it.label }
    val helped = stringResource(R.string.migraine_helped_me)

    DetailLine(
        R.string.history_migraine_where,
        entry.areas.map { stringResource(areaLabel(it)) } + entry.painCharacters.map { stringResource(painLabel(it)) }
    )
    DetailLine(R.string.history_migraine_type, listOfNotNull(entry.type?.let { stringResource(typeLabel(it)) }))
    DetailLine(R.string.history_migraine_symptoms, entry.symptoms.map { stringResource(symptomLabel(it)) })
    DetailLine(
        R.string.history_migraine_triggers,
        entry.triggers.map { stringResource(triggerLabel(it)) } + entry.customTriggerIds.mapNotNull { labelOf[it] }
    )
    if (entry.tookSomething) {
        val taken = buildList {
            add(entry.medicationText ?: stringResource(R.string.history_migraine_took_something))
            if (entry.secondDose) add(stringResource(R.string.history_migraine_second_dose))
            entry.intensityAfter2Hours?.let { add(stringResource(R.string.history_migraine_after_2h, it)) }
            if (entry.medicationHelpedMe) add(helped)
        }
        DetailLine(R.string.history_migraine_taken, taken)
    }
    val relief = entry.reliefMethods.map { stringResource(reliefLabel(it)) } + entry.customReliefIds.mapNotNull { labelOf[it] }
    DetailLine(R.string.history_migraine_relief, if (entry.reliefHelpedMe && relief.isNotEmpty()) relief + helped else relief)
    DetailLine(R.string.history_migraine_activities, listOfNotNull(entry.activities?.let { stringResource(activitiesLabel(it)) }))
    CommentLine(entry.comment)
    if (entry.ongoing) {
        TextButton(onClick = onEnd) { Text(stringResource(R.string.history_migraine_end_now)) }
    }
}

@Composable
private fun DetailLine(label: Int, values: List<String>) {
    if (values.isEmpty()) return
    Text(stringResource(label, values.joinToString(", ")), style = MaterialTheme.typography.bodyMedium)
}

private fun activitiesLabel(a: DailyActivities) =
    if (a == DailyActivities.COULD_CARRY_ON) R.string.migraine_could_carry_on else R.string.migraine_had_to_stop
