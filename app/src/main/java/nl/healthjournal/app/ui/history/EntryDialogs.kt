package nl.healthjournal.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.distanceFromMeters
import nl.healthjournal.app.ui.common.distanceToMeters
import nl.healthjournal.app.ui.common.formatInput
import nl.healthjournal.app.ui.common.glucoseDigits
import nl.healthjournal.app.ui.common.glucoseFromMmol
import nl.healthjournal.app.ui.common.glucoseToMmol
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.app.ui.common.weightFromKg
import nl.healthjournal.app.ui.common.weightToKg
import nl.healthjournal.domain.model.common.UnitConversion
import nl.healthjournal.domain.model.metrics.GlucoseContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A History entry card with Edit and Delete icon buttons. The buttons are [IconButton]s, which
 * give the Material minimum 48 dp touch target, and carry localized content descriptions.
 */
@Composable
fun EntryCard(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp), content = content)
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.history_edit_entry))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.history_delete_entry),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Shows the delete confirmation and the edit dialog for whichever entry the state points at. */
@Composable
fun EntryDialogs(state: HistoryUiState, viewModel: HistoryViewModel) {
    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    state.editing?.let { ref ->
        // Keyed on the id so switching entries never reuses stale field state.
        key(ref.id) { EditEntryDialog(ref, state.editError, viewModel) }
    }
}

@Composable
private fun EditEntryDialog(ref: EntryRef, serverError: UiText?, viewModel: HistoryViewModel) {
    val zone = remember { ZoneId.systemDefault() }
    val units = LocalDisplayUnits.current
    val initialTime: Instant = when (ref) {
        is EntryRef.Weight -> ref.entry.timestamp
        is EntryRef.BloodPressure -> ref.entry.timestamp
        is EntryRef.Glucose -> ref.entry.timestamp
        is EntryRef.Activity -> ref.session.startTime
    }
    var dateTime by remember { mutableStateOf(LocalDateTime.ofInstant(initialTime, zone)) }
    var invalid by remember { mutableStateOf(false) }

    // Fields show the stored metric values converted to the display units. A field left untouched keeps
    // the stored value, so display rounding never alters data that was not edited (ADR 0014).
    val initialWeight = remember {
        (ref as? EntryRef.Weight)?.entry?.weight?.value?.let { formatInput(units.weightFromKg(it.toDouble()), 2) }.orEmpty()
    }
    val initialGlucose = remember {
        (ref as? EntryRef.Glucose)?.entry?.glucose?.valueInMmolL?.let { formatInput(units.glucoseFromMmol(it.toDouble()), units.glucoseDigits) }.orEmpty()
    }
    val initialDistance = remember {
        (ref as? EntryRef.Activity)?.session?.let { formatInput(units.distanceFromMeters(it.distanceInMeters), 2) }.orEmpty()
    }
    var weight by remember { mutableStateOf(initialWeight) }
    var systolic by remember { mutableStateOf((ref as? EntryRef.BloodPressure)?.entry?.reading?.systolic?.toString().orEmpty()) }
    var diastolic by remember { mutableStateOf((ref as? EntryRef.BloodPressure)?.entry?.reading?.diastolic?.toString().orEmpty()) }
    var glucose by remember { mutableStateOf(initialGlucose) }
    var glucoseContext by remember { mutableStateOf((ref as? EntryRef.Glucose)?.entry?.context ?: GlucoseContext.FASTING) }
    var duration by remember {
        mutableStateOf((ref as? EntryRef.Activity)?.session?.let { formatInput(it.durationInSeconds / 60.0, 2) }.orEmpty())
    }
    var distance by remember { mutableStateOf(initialDistance) }

    fun save() {
        invalid = false
        when (ref) {
            is EntryRef.Weight -> {
                val kg = if (weight == initialWeight) ref.entry.weight.value.toDouble() else weight.parseDecimal()?.let { units.weightToKg(it) }
                if (kg == null || kg !in 1.0..700.0) invalid = true
                else {
                    val stored = if (weight == initialWeight) ref.entry.weight.value else UnitConversion.toStoredKg(kg)
                    viewModel.updateWeight(ref.entry, stored, dateTime.atZone(zone).toInstant())
                }
            }
            is EntryRef.BloodPressure -> {
                val sys = systolic.toIntOrNull()?.takeIf { it in 40..300 }
                val dia = diastolic.toIntOrNull()?.takeIf { it in 20..200 }
                if (sys == null || dia == null || sys <= dia) invalid = true
                else viewModel.updateBloodPressure(ref.entry, sys, dia, dateTime.atZone(zone).toInstant())
            }
            is EntryRef.Glucose -> {
                val unchanged = glucose == initialGlucose
                val mmol = if (unchanged) ref.entry.glucose.valueInMmolL.toDouble() else glucose.parseDecimal()?.let { units.glucoseToMmol(it) }
                if (mmol == null || mmol <= 0) invalid = true
                else {
                    val stored = if (unchanged) ref.entry.glucose.valueInMmolL else BigDecimal.valueOf(mmol).setScale(2, java.math.RoundingMode.HALF_UP)
                    viewModel.updateGlucose(ref.entry, glucoseContext, stored, dateTime.atZone(zone).toInstant())
                }
            }
            is EntryRef.Activity -> {
                val minutes = duration.parseDecimal()?.takeIf { it > 0 }
                val meters = if (distance == initialDistance) ref.session.distanceInMeters
                else distance.parseDecimal()?.takeIf { it >= 0 }?.let { units.distanceToMeters(it) }
                if (minutes == null || meters == null) invalid = true
                else viewModel.updateActivity(ref.session, Math.round(minutes * 60), meters)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.cancelEdit() },
        title = { Text(stringResource(R.string.history_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (ref) {
                    is EntryRef.Weight -> DecimalField(weight, { weight = it }, stringResource(R.string.log_weight_label, units.weightSymbol))
                    is EntryRef.BloodPressure -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = systolic,
                            onValueChange = { systolic = it },
                            label = { Text(stringResource(R.string.log_systolic_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = diastolic,
                            onValueChange = { diastolic = it },
                            label = { Text(stringResource(R.string.log_diastolic_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    is EntryRef.Glucose -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = glucoseContext == GlucoseContext.FASTING,
                                onClick = { glucoseContext = GlucoseContext.FASTING },
                                label = { Text(stringResource(R.string.log_glucose_context_fasting)) }
                            )
                            FilterChip(
                                selected = glucoseContext == GlucoseContext.POSTPRANDIAL,
                                onClick = { glucoseContext = GlucoseContext.POSTPRANDIAL },
                                label = { Text(stringResource(R.string.log_glucose_context_postprandial)) }
                            )
                        }
                        DecimalField(glucose, { glucose = it }, stringResource(R.string.log_glucose_label, units.glucoseSymbol))
                    }
                    is EntryRef.Activity -> {
                        DecimalField(duration, { duration = it }, stringResource(R.string.log_duration_label))
                        DecimalField(distance, { distance = it }, stringResource(R.string.log_distance_label, units.distanceSymbol))
                    }
                }

                // Activities keep their start time (the duration edits the end time).
                if (ref is EntryRef.Activity) {
                    Text(
                        stringResource(R.string.history_start_line, formatDateTime(dateTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    DateTimeFields(dateTime) { dateTime = it }
                }

                if (invalid) {
                    Text(
                        stringResource(R.string.history_edit_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                serverError?.let {
                    Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { Button(onClick = ::save) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = { viewModel.cancelEdit() }) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
private fun DecimalField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeFields(value: LocalDateTime, onChange: (LocalDateTime) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
            Text(value.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), maxLines = 1)
        }
        OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
            Text(value.toLocalTime().format(TIME_FORMAT), maxLines = 1)
        }
    }

    if (showDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val date = LocalDate.ofInstant(Instant.ofEpochMilli(ms), ZoneOffset.UTC)
                        onChange(LocalDateTime.of(date, value.toLocalTime()))
                    }
                    showDate = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }

    if (showTime) {
        val pickerState = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalDateTime.of(value.toLocalDate(), LocalTime.of(pickerState.hour, pickerState.minute)))
                    showTime = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatDateTime(value: LocalDateTime): String =
    value.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
