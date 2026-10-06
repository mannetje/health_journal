package nl.healthjournal.app.ui.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.CommentField
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.model.metrics.EntryComment
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Add or edit one medication. Text stays text until Save; the ViewModel validates and reports problems. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationEditScreen(
    initial: MedicationDraft,
    isArchived: Boolean,
    onSave: (MedicationDraft) -> Unit,
    onArchive: (MedicationId) -> Unit,
    onDelete: (MedicationId) -> Unit,
    onCancel: () -> Unit
) {
    var draft by remember { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    val isNew = initial.id == null

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(if (isNew) R.string.medication_edit_title_new else R.string.medication_edit_title_edit),
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = draft.name,
            onValueChange = { if (it.length <= MedicationName.MAX_LENGTH) draft = draft.copy(name = it) },
            label = { Text(stringResource(R.string.medication_field_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SectionTitle(R.string.medication_field_form)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip(stringResource(R.string.common_not_set), draft.form == null) { draft = draft.copy(form = null) }
            MedicationForm.entries.forEach { form ->
                Chip(stringResource(form.labelRes()), draft.form == form) { draft = draft.copy(form = form) }
            }
        }

        SectionTitle(R.string.medication_field_strength)
        DecimalField(draft.strengthText, R.string.medication_field_strength_amount) { draft = draft.copy(strengthText = it) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StrengthUnit.entries.forEach { unit ->
                Chip(stringResource(unit.labelRes()), draft.strengthUnit == unit) { draft = draft.copy(strengthUnit = unit) }
            }
        }

        SectionTitle(R.string.medication_field_dose)
        DecimalField(draft.doseText, R.string.medication_field_dose_amount) { draft = draft.copy(doseText = it) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DoseUnit.entries.forEach { unit ->
                Chip(stringResource(unit.nameRes()), draft.doseUnit == unit) { draft = draft.copy(doseUnit = unit) }
            }
        }

        ScheduleSection(draft, isNew) { draft = it }

        SectionTitle(R.string.medication_field_appearance)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PillColor.entries.forEach { color ->
                ColorChip(color, draft.color == color) { draft = draft.copy(color = if (draft.color == color) null else color) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PillShape.entries.forEach { shape ->
                ShapeChip(shape, draft.color, draft.shape == shape) { draft = draft.copy(shape = if (draft.shape == shape) null else shape) }
            }
        }

        CommentField(
            value = draft.comment,
            onValueChange = { if (it.length <= EntryComment.MAX_LENGTH) draft = draft.copy(comment = it) }
        )

        Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.common_save), textAlign = TextAlign.Center)
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.common_cancel), textAlign = TextAlign.Center)
        }
        initial.id?.let { id ->
            if (!isArchived) {
                OutlinedButton(onClick = { confirmArchive = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medication_action_archive), textAlign = TextAlign.Center)
                }
            }
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text(
                    stringResource(R.string.medication_action_delete),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            if (confirmArchive) {
                ConfirmDialog(
                    title = R.string.medication_archive_title,
                    text = R.string.medication_archive_text,
                    confirm = R.string.medication_action_archive,
                    onConfirm = { confirmArchive = false; onArchive(id) },
                    onDismiss = { confirmArchive = false }
                )
            }
            if (confirmDelete) {
                ConfirmDialog(
                    title = R.string.medication_delete_title,
                    text = R.string.medication_delete_text,
                    confirm = R.string.medication_action_delete,
                    onConfirm = { confirmDelete = false; onDelete(id) },
                    onDismiss = { confirmDelete = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSection(draft: MedicationDraft, isNew: Boolean, onChange: (MedicationDraft) -> Unit) {
    var timeDialog by remember { mutableStateOf<LocalTime?>(null) }
    var addingTime by remember { mutableStateOf(false) }
    var dateDialog by remember { mutableStateOf(false) }

    SectionTitle(R.string.medication_field_schedule)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.medication_as_needed), modifier = Modifier.weight(1f))
        Switch(checked = draft.asNeeded, onCheckedChange = { onChange(draft.copy(asNeeded = it)) })
    }
    if (!draft.asNeeded) {
        draft.times.forEach { time ->
            val label = time.format(TIME_FORMAT)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { timeDialog = time }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(label) }
                val removeDescription = stringResource(R.string.medication_remove_time_desc, label)
                TextButton(
                    onClick = { onChange(draft.copy(times = draft.times - time)) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = removeDescription }
                ) { Text(stringResource(R.string.medication_action_remove_time)) }
            }
        }
        OutlinedButton(
            onClick = { addingTime = true },
            enabled = draft.times.size < nl.healthjournal.domain.model.medication.Schedule.Recurring.MAX_TIMES_PER_DAY,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text(stringResource(R.string.medication_action_add_time), textAlign = TextAlign.Center) }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Chip(stringResource(R.string.medication_pattern_weekdays), !draft.everyNDays) { onChange(draft.copy(everyNDays = false)) }
            Chip(stringResource(R.string.medication_pattern_every_n_days), draft.everyNDays) { onChange(draft.copy(everyNDays = true)) }
        }
        if (draft.everyNDays) {
            OutlinedTextField(
                value = draft.intervalText,
                onValueChange = { onChange(draft.copy(intervalText = it.filter(Char::isDigit).take(3))) },
                label = { Text(stringResource(R.string.medication_field_interval)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { day ->
                    Chip(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), day in draft.weekdays) {
                        onChange(draft.copy(weekdays = if (day in draft.weekdays) draft.weekdays - day else draft.weekdays + day))
                    }
                }
            }
        }
    }

    if (!isNew) {
        val applyFrom = draft.applyFrom ?: LocalDate.now()
        OutlinedButton(onClick = { dateDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(
                stringResource(R.string.medication_field_apply_from, applyFrom.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))),
                textAlign = TextAlign.Center
            )
        }
        if (applyFrom.isBefore(LocalDate.now())) {
            Text(stringResource(R.string.medication_apply_from_past_note), style = MaterialTheme.typography.bodySmall)
        }
    }

    if (addingTime || timeDialog != null) {
        val start = timeDialog ?: LocalTime.of(8, 0)
        val state = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = true)
        val close = { addingTime = false; timeDialog = null }
        AlertDialog(
            onDismissRequest = close,
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val picked = LocalTime.of(state.hour, state.minute)
                    val old = timeDialog
                    val updated = (if (old != null) draft.times - old else draft.times) + picked
                    onChange(draft.copy(times = updated.distinct().sorted()))
                    close()
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    if (dateDialog) {
        val current = draft.applyFrom ?: LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { dateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        onChange(draft.copy(applyFrom = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()))
                    }
                    dateDialog = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { dateDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun DecimalField(value: String, labelRes: Int, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        // Both a comma and a point are accepted; the ViewModel parses either.
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(10)) },
        label = { Text(stringResource(labelRes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

@Composable
private fun ColorChip(color: PillColor, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(color.labelRes())) },
        leadingIcon = {
            Box(
                Modifier
                    .size(16.dp)
                    .background(color.toColor(), CircleShape)
            )
        },
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

@Composable
private fun ShapeChip(shape: PillShape, color: PillColor?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(shape.labelRes())) },
        leadingIcon = { PillIcon(color?.let { PillAppearance(it, shape) } ?: PillAppearance(PillColor.GREY, shape), size = 20.dp) },
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

@Composable
private fun ConfirmDialog(title: Int, text: Int, confirm: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(text)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
