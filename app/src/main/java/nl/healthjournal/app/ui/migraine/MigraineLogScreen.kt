package nl.healthjournal.app.ui.migraine

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.CommentField
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.app.ui.history.DateTimeFields
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The migraine tab of the Log screen: the newest entry as plain text and the button that opens the form. */
@Composable
fun MigraineCard(state: MigraineUiState, onLog: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.migraine_card_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            state.successMessage?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            val latest = state.latest
            Text(
                if (latest == null) stringResource(R.string.migraine_card_empty)
                else stringResource(R.string.migraine_card_latest, describeLatest(latest)),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onLog,
                enabled = state.activeProfile != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.migraine_log_button)) }
        }
    }
}

@Composable
private fun describeLatest(entry: MigraineEntry): String {
    val start = entry.start.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    return if (entry.ongoing) "$start, ${stringResource(R.string.migraine_card_still_going)}" else start
}

/** The nine-step form. Shown inside the migraine tab of the Log screen while [MigraineUiState.open]; the screen scrolls it. */
@Composable
fun MigraineLogScreen(viewModel: MigraineLogViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val draft = state.draft ?: return
    val last = MigraineStep.entries.last()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // A new entry needs no title: the Migraine tab or filter already says what is being logged.
        if (draft.editingId != null) {
            Text(
                stringResource(R.string.migraine_title_edit),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
        }
        Text(
            stringResource(R.string.migraine_step_progress, state.step.ordinal + 1, MigraineStep.entries.size),
            style = MaterialTheme.typography.labelLarge
        )
        LinearProgressIndicator(
            progress = { (state.step.ordinal + 1f) / MigraineStep.entries.size },
            modifier = Modifier.fillMaxWidth()
        )

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (state.step) {
                MigraineStep.TIME -> TimeStep(draft, viewModel)
                MigraineStep.INTENSITY -> IntensityStep(draft, viewModel)
                MigraineStep.WHERE -> WhereStep(draft, viewModel)
                MigraineStep.TYPE -> TypeStep(draft, viewModel)
                MigraineStep.SYMPTOMS -> SymptomsStep(draft, viewModel)
                MigraineStep.TRIGGERS -> TriggersStep(draft, state, viewModel)
                MigraineStep.MEDICATION -> MedicationStep(draft, viewModel)
                MigraineStep.RELIEF -> ReliefStep(draft, state, viewModel)
                MigraineStep.COMMENT -> CommentStep(draft, viewModel)
            }
        }

        state.errorMessage?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.migraine_back), maxLines = 1)
            }
            if (state.step != MigraineStep.TIME && state.step != last) {
                TextButton(onClick = viewModel::skip) { Text(stringResource(R.string.migraine_skip), maxLines = 1) }
            }
            if (state.step == last) {
                Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_save), maxLines = 1)
                }
            } else {
                Button(onClick = viewModel::next, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.migraine_next), maxLines = 1)
                }
            }
        }
        // Save is available from every step once the person has filled in what they want.
        if (state.step != last) {
            TextButton(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }
        }
    }

    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDiscardPrompt,
            title = { Text(stringResource(R.string.migraine_discard_title)) },
            text = { Text(stringResource(R.string.migraine_discard_body)) },
            confirmButton = { TextButton(onClick = viewModel::discard) { Text(stringResource(R.string.migraine_discard_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDiscardPrompt) { Text(stringResource(R.string.migraine_discard_keep)) } }
        )
    }
    state.tagDialog?.let { TagDialog(it, viewModel) }
}

// ---- steps

@Composable
private fun StepTitle(@StringRes id: Int) {
    Text(stringResource(id), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TimeStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_started)
    DateTimeFields(d.start, vm::setStart)
    SwitchRow(R.string.migraine_woke_up, d.wokeUpWithIt, vm::setWokeUpWithIt)
    StepTitle(R.string.migraine_ended)
    SwitchRow(R.string.migraine_still_going, d.end == null, vm::setStillGoing)
    d.end?.let { DateTimeFields(it, vm::setEnd) }
}

@Composable
private fun IntensityStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_pain_score)
    ScorePicker(d.intensity, vm::setIntensity)
}

/** The 0 to 10 picker; tapping the chosen number again, or "No answer", leaves it empty. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScorePicker(value: Int?, onChange: (Int?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (n in 0..10) {
            FilterChip(
                selected = value == n,
                onClick = { onChange(if (value == n) null else n) },
                label = { Text(n.toString()) },
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }
    if (value != null) {
        TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.migraine_score_clear)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(items: List<T>, selected: Set<T>, label: @Composable (T) -> String, onToggle: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(
                selected = item in selected,
                onClick = { onToggle(item) },
                label = { Text(label(item)) },
                modifier = Modifier.heightIn(min = 48.dp)
            )
        }
    }
}

@Composable
private fun WhereStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_where)
    ChipGroup(HeadArea.entries, d.areas, { stringResource(areaLabel(it)) }, vm::toggleArea)
    StepTitle(R.string.migraine_feels_like)
    ChipGroup(PainCharacter.entries, d.painCharacters, { stringResource(painLabel(it)) }, vm::togglePainCharacter)
}

@Composable
private fun TypeStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_type_title)
    ChipGroup(MigraineType.entries, setOfNotNull(d.type), { stringResource(typeLabel(it)) }, vm::setType)
}

@Composable
private fun SymptomsStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_symptoms_title)
    ChipGroup(MigraineSymptom.entries, d.symptoms, { stringResource(symptomLabel(it)) }, vm::toggleSymptom)
}

@Composable
private fun TriggersStep(d: MigraineDraft, state: MigraineUiState, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_triggers_title)
    ChipGroup(MigraineTrigger.entries, d.triggers, { stringResource(triggerLabel(it)) }, vm::toggleTrigger)
    PersonalTags(MigraineTagKind.TRIGGER, d.customTriggerIds, state, vm, vm::toggleCustomTrigger)
}

@Composable
private fun MedicationStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_medication_title)
    SwitchRow(R.string.migraine_took_something, d.tookSomething, vm::setTookSomething)
    if (d.tookSomething) {
        OutlinedTextField(
            value = d.medicationText,
            onValueChange = vm::setMedicationText,
            label = { Text(stringResource(R.string.migraine_what_i_took)) },
            placeholder = { Text(stringResource(R.string.migraine_what_i_took_hint)) },
            supportingText = { Text(stringResource(R.string.comment_counter, d.medicationText.length, MigraineLogViewModel.MEDICATION_MAX)) },
            singleLine = true,
            // No suggestions or autofill: the app never offers a medicine name.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )
        SwitchRow(R.string.migraine_second_dose, d.secondDose, vm::setSecondDose)
        Text(stringResource(R.string.migraine_pain_after_2h), style = MaterialTheme.typography.titleSmall)
        ScorePicker(d.intensityAfter2Hours, vm::setIntensityAfter2Hours)
        SwitchRow(R.string.migraine_helped_me, d.medicationHelpedMe, vm::setMedicationHelpedMe)
    }
}

@Composable
private fun ReliefStep(d: MigraineDraft, state: MigraineUiState, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_relief_title)
    ChipGroup(ReliefMethod.entries, d.reliefMethods, { stringResource(reliefLabel(it)) }, vm::toggleRelief)
    PersonalTags(MigraineTagKind.RELIEF, d.customReliefIds, state, vm, vm::toggleCustomRelief)
    if (d.hasRelief) SwitchRow(R.string.migraine_helped_me, d.reliefHelpedMe, vm::setReliefHelpedMe)
    ChipGroup(
        DailyActivities.entries, setOfNotNull(d.activities),
        { stringResource(if (it == DailyActivities.COULD_CARRY_ON) R.string.migraine_could_carry_on else R.string.migraine_had_to_stop) },
        vm::setActivities
    )
}

@Composable
private fun CommentStep(d: MigraineDraft, vm: MigraineLogViewModel) {
    StepTitle(R.string.migraine_comment_title)
    CommentField(value = d.comment, onValueChange = vm::setComment)
}

// ---- personal tags

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalTags(
    kind: MigraineTagKind,
    selected: Set<nl.healthjournal.domain.model.migraine.MigraineTagId>,
    state: MigraineUiState,
    vm: MigraineLogViewModel,
    onToggle: (nl.healthjournal.domain.model.migraine.MigraineTagId) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.activeTags(kind).forEach { tag ->
            PersonalTagChip(tag, tag.id in selected, onToggle = { onToggle(tag.id) }, onOpen = { vm.openTag(tag) })
        }
        AssistChip(
            onClick = { vm.openAddTag(kind) },
            label = { Text(stringResource(R.string.migraine_tag_add_own)) },
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

/** Tap selects. Long-press, or the TalkBack action, opens rename and remove. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonalTagChip(tag: MigraineTag, selected: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    val actionLabel = stringResource(R.string.migraine_tag_actions, tag.label)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics {
                this.selected = selected
                customActions = listOf(CustomAccessibilityAction(actionLabel) { onOpen(); true })
            }
            .combinedClickable(role = Role.Checkbox, onClick = onToggle, onLongClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
            }
            Text(tag.label, style = MaterialTheme.typography.labelLarge, maxLines = 2)
        }
    }
}

@Composable
private fun TagDialog(dialog: TagDialogState, vm: MigraineLogViewModel) {
    val editing = dialog.tag != null
    AlertDialog(
        onDismissRequest = vm::closeTagDialog,
        title = { Text(stringResource(if (editing) R.string.migraine_tag_rename else R.string.migraine_tag_add_own)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dialog.input,
                    onValueChange = vm::onTagInput,
                    label = { Text(stringResource(R.string.migraine_tag_name)) },
                    singleLine = true,
                    isError = dialog.error != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                dialog.error?.let {
                    Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (editing) {
                    Text(stringResource(R.string.migraine_tag_rename_note), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = vm::removeTag) { Text(stringResource(R.string.migraine_tag_remove)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::confirmTag) { Text(stringResource(R.string.common_save)) } },
        dismissButton = { TextButton(onClick = vm::closeTagDialog) { Text(stringResource(R.string.common_cancel)) } }
    )
}

// ---- labels

@StringRes
internal fun areaLabel(a: HeadArea) = when (a) {
    HeadArea.WHOLE_HEAD -> R.string.migraine_area_whole_head
    HeadArea.LEFT_SIDE -> R.string.migraine_area_left_side
    HeadArea.RIGHT_SIDE -> R.string.migraine_area_right_side
    HeadArea.FOREHEAD -> R.string.migraine_area_forehead
    HeadArea.BEHIND_EYES -> R.string.migraine_area_behind_eyes
    HeadArea.TEMPLES -> R.string.migraine_area_temples
    HeadArea.BACK_OF_HEAD_AND_NECK -> R.string.migraine_area_back_of_head_and_neck
    HeadArea.OTHER -> R.string.migraine_area_other
}

@StringRes
internal fun painLabel(p: PainCharacter) = when (p) {
    PainCharacter.THROBBING -> R.string.migraine_pain_throbbing
    PainCharacter.PRESSING -> R.string.migraine_pain_pressing
    PainCharacter.PIERCING -> R.string.migraine_pain_piercing
}

@StringRes
internal fun typeLabel(t: MigraineType) = when (t) {
    MigraineType.WITH_AURA -> R.string.migraine_type_with_aura
    MigraineType.WITHOUT_AURA -> R.string.migraine_type_without_aura
    MigraineType.RECOGNIZED_HEADACHE -> R.string.migraine_type_recognized_headache
    MigraineType.OTHER_HEADACHE -> R.string.migraine_type_other_headache
    MigraineType.NOT_SURE -> R.string.migraine_type_not_sure
}

@StringRes
internal fun symptomLabel(s: MigraineSymptom) = when (s) {
    MigraineSymptom.NAUSEA -> R.string.migraine_symptom_nausea
    MigraineSymptom.AURA -> R.string.migraine_symptom_aura
    MigraineSymptom.SENSITIVE_TO_LIGHT -> R.string.migraine_symptom_sensitive_to_light
    MigraineSymptom.SENSITIVE_TO_SOUND -> R.string.migraine_symptom_sensitive_to_sound
    MigraineSymptom.SENSITIVE_TO_SMELL -> R.string.migraine_symptom_sensitive_to_smell
    MigraineSymptom.DIZZINESS -> R.string.migraine_symptom_dizziness
    MigraineSymptom.VERTIGO -> R.string.migraine_symptom_vertigo
    MigraineSymptom.MOVEMENT_SYMPTOMS -> R.string.migraine_symptom_movement_symptoms
    MigraineSymptom.WORSE_WITH_ACTIVITY -> R.string.migraine_symptom_worse_with_activity
    MigraineSymptom.BRAIN_FOG -> R.string.migraine_symptom_brain_fog
    MigraineSymptom.EXTREME_FATIGUE -> R.string.migraine_symptom_extreme_fatigue
    MigraineSymptom.OTHER -> R.string.migraine_symptom_other
}

@StringRes
internal fun triggerLabel(t: MigraineTrigger) = when (t) {
    MigraineTrigger.SLEEP -> R.string.migraine_trigger_sleep
    MigraineTrigger.STRESS -> R.string.migraine_trigger_stress
    MigraineTrigger.FOOD -> R.string.migraine_trigger_food
    MigraineTrigger.WEATHER -> R.string.migraine_trigger_weather
    MigraineTrigger.MENSTRUATION -> R.string.migraine_trigger_menstruation
    MigraineTrigger.OTHER -> R.string.migraine_trigger_other
}

@StringRes
internal fun reliefLabel(r: ReliefMethod) = when (r) {
    ReliefMethod.SLEEP_OR_REST -> R.string.migraine_relief_sleep_or_rest
    ReliefMethod.DARK_QUIET_ROOM -> R.string.migraine_relief_dark_quiet_room
    ReliefMethod.COLD_PACK -> R.string.migraine_relief_cold_pack
    ReliefMethod.HEAT -> R.string.migraine_relief_heat
    ReliefMethod.MASSAGE -> R.string.migraine_relief_massage
    ReliefMethod.COFFEE_OR_TEA -> R.string.migraine_relief_coffee_or_tea
    ReliefMethod.OTHER -> R.string.migraine_relief_other
}
