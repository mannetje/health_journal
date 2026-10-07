package nl.healthjournal.app.ui.medication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.PlannedStatus
import nl.healthjournal.domain.model.medication.Slot
import nl.healthjournal.domain.model.medication.SlotItem
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

/** The pillbox: a switch between today's intakes and the medication list, plus the add and edit form. */
@Composable
fun PillboxScreen(
    viewModel: MedicationViewModel,
    showNotice: Boolean,
    onNoticeAccepted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    // A draft being edited replaces the list; null shows the list.
    var editing by remember { mutableStateOf<MedicationDraft?>(null) }

    if (showNotice) {
        MedicationNoticeDialog(confirmLabel = stringResource(R.string.medication_notice_accept), onConfirm = onNoticeAccepted)
    }

    val draft = editing
    if (draft != null) {
        BackHandler { editing = null }
        Column(Modifier.fillMaxWidth()) {
            MessageBanner(state.errorMessage, state.successMessage)
            MedicationEditScreen(
                initial = draft,
                onSave = { viewModel.saveMedication(it) { editing = null } },
                onArchive = { id -> viewModel.archive(id); editing = null },
                onDelete = { id -> viewModel.delete(id); editing = null },
                onCancel = { editing = null },
                isArchived = draft.id?.let { id -> state.medications.firstOrNull { it.id == id }?.archivedFrom != null } ?: false
            )
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        PillboxSwitch(tab) { tab = it }
        MessageBanner(state.errorMessage, state.successMessage)
        if (state.profileId == null && !state.isLoading) {
            Text(
                stringResource(R.string.medication_no_profile),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        } else if (tab == 0) {
            TodayView(state, viewModel)
        } else {
            MedicationListView(
                medications = state.medications,
                onAdd = { editing = MedicationDraft() },
                onEdit = { editing = MedicationDraft.from(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillboxSwitch(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(R.string.medication_tab_today, R.string.medication_tab_medications)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = selected == index,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(label), textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun MessageBanner(error: nl.healthjournal.app.ui.common.UiText?, success: nl.healthjournal.app.ui.common.UiText?) {
    val (text, container) = when {
        error != null -> error to MaterialTheme.colorScheme.errorContainer
        success != null -> success to MaterialTheme.colorScheme.primaryContainer
        else -> return
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) { Text(text.asString(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun TodayView(state: MedicationUiState, viewModel: MedicationViewModel) {
    val day = state.day
    var selectedItem by remember { mutableStateOf<SlotItem?>(null) }
    val grouped = day?.slots.orEmpty().groupBy { TimeOfDay.of(it.time.toLocalTime()) }
    val asNeeded = state.medications.filter { it.isAsNeeded && it.archivedFrom == null }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { WeekStrip(state.week, state.date, viewModel::selectDate) }
        if (state.date != state.today) {
            item {
                OutlinedButton(onClick = { viewModel.selectDate(state.today) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medication_back_to_today))
                }
            }
        }
        if (day != null && day.slots.isEmpty() && day.logged.isEmpty() && asNeeded.isEmpty()) {
            item {
                Text(stringResource(R.string.medication_today_empty), style = MaterialTheme.typography.bodyLarge)
            }
        }
        TimeOfDay.entries.forEach { part ->
            val slots = grouped[part].orEmpty()
            if (slots.isNotEmpty()) {
                item(key = "header-$part") {
                    Text(stringResource(part.labelRes()), style = MaterialTheme.typography.titleMedium)
                }
                items(slots, key = { it.time.toString() }) { slot ->
                    SlotCard(slot, onTakeAll = { viewModel.takeSlot(slot.time) }, onItemClick = { selectedItem = it })
                }
            }
        }
        if (asNeeded.isNotEmpty()) {
            item { Text(stringResource(R.string.medication_as_needed_title), style = MaterialTheme.typography.titleMedium) }
            items(asNeeded, key = { "asneeded-${it.id}" }) { medication ->
                AsNeededRow(medication, onLog = { viewModel.logAsNeeded(medication.id) })
            }
        }
        if (day != null && day.logged.isNotEmpty()) {
            item { Text(stringResource(R.string.medication_logged_title), style = MaterialTheme.typography.titleMedium) }
            items(day.logged, key = { "logged-${it.id}" }) { intake ->
                val medication = state.medications.firstOrNull { it.id == intake.medicationId }
                if (medication != null) {
                    LoggedRow(medication, intake)
                }
            }
        }
    }

    selectedItem?.let { item ->
        IntakeDialog(
            item = item,
            onTaken = { viewModel.recordIntake(item.medication.id, item.planned, IntakeStatus.TAKEN); selectedItem = null },
            onSkip = { viewModel.recordIntake(item.medication.id, item.planned, IntakeStatus.SKIPPED); selectedItem = null },
            onDismiss = { selectedItem = null }
        )
    }
}

@Composable
private fun WeekStrip(week: List<WeekDay>, selected: java.time.LocalDate, onSelect: (java.time.LocalDate) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { weekDay ->
            val name = weekDay.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            val dateText = weekDay.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            val statusText = stringResource(weekDay.status.labelRes())
            val symbol = when (weekDay.status) {
                DayStatus.COMPLETE -> "✔"
                DayStatus.PARTIAL -> "◐"
                DayStatus.MISSED -> "✖"
                DayStatus.EMPTY -> "○"
            }
            val isSelected = weekDay.date == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(weekDay.date) }
                    .semantics { contentDescription = "$name $dateText: $statusText" }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(symbol, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SlotCard(slot: Slot, onTakeAll: () -> Unit, onItemClick: (SlotItem) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(slot.time.toLocalTime().format(TIME_FORMAT), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            slot.items.forEach { SlotRow(it, onClick = { onItemClick(it) }) }
            if (slot.openItems.isNotEmpty()) {
                Button(onClick = onTakeAll, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medication_action_taken_all), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SlotRow(item: SlotItem, onClick: () -> Unit) {
    val medication = item.medication
    val dose = medication.dosage.doseText()
    val status = stringResource(item.status.labelRes())
    val description = "${medication.name.value}, $dose, $status"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = stringResource(R.string.medication_row_click_label), onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PillIcon(medication.appearance)
        Column(Modifier.weight(1f)) {
            Text(medication.name.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(dose, style = MaterialTheme.typography.bodyMedium)
            StatusChip(item.status)
        }
    }
}

@Composable
private fun StatusChip(status: PlannedStatus) {
    val container = when (status) {
        PlannedStatus.TAKEN -> MaterialTheme.colorScheme.primaryContainer
        PlannedStatus.MISSED -> MaterialTheme.colorScheme.errorContainer
        PlannedStatus.SKIPPED -> MaterialTheme.colorScheme.tertiaryContainer
        PlannedStatus.PENDING -> MaterialTheme.colorScheme.surface
    }
    Surface(color = container, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(stringResource(status.labelRes()), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun AsNeededRow(medication: Medication, onLog: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillIcon(medication.appearance)
            Column(Modifier.weight(1f)) {
                Text(medication.name.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(medication.dosage.doseText(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onLog, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.medication_action_log_dose), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LoggedRow(medication: Medication, intake: nl.healthjournal.domain.model.medication.Intake) {
    val time = (intake.takenAt?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: intake.planned?.toLocalTime() ?: LocalTime.MIDNIGHT).format(TIME_FORMAT)
    val status = stringResource(
        if (intake.status == IntakeStatus.TAKEN) R.string.medication_status_taken else R.string.medication_status_skipped
    )
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) {
            contentDescription = "${medication.name.value}, $status, $time"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PillIcon(medication.appearance, size = 24.dp)
        Text("${medication.name.value} · $status · $time", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun IntakeDialog(item: SlotItem, onTaken: () -> Unit, onSkip: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.medication.name.value) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${item.planned.toLocalTime().format(TIME_FORMAT)} · ${item.medication.dosage.doseText()}")
                Button(onClick = onTaken, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medication_action_taken))
                }
                OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.medication_action_skip))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
