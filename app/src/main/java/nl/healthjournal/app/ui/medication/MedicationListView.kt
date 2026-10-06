package nl.healthjournal.app.ui.medication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.medication.Medication

/** All medications of the active profile, archived ones included and marked. */
@Composable
fun MedicationListView(
    medications: List<Medication>,
    onAdd: () -> Unit,
    onEdit: (Medication) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.medication_action_add), textAlign = TextAlign.Center)
            }
        }
        if (medications.isEmpty()) {
            item { Text(stringResource(R.string.medication_list_empty), style = MaterialTheme.typography.bodyLarge) }
        }
        items(medications, key = { it.id.toString() }) { medication ->
            MedicationCard(medication, onClick = { onEdit(medication) })
        }
    }
}

@Composable
private fun MedicationCard(medication: Medication, onClick: () -> Unit) {
    val dose = medication.dosage.doseText()
    val strength = medication.dosage.strengthText()
    val frequency = medication.frequencyText()
    val archived = medication.archivedFrom != null
    val archivedText = stringResource(R.string.medication_archived_badge)
    val description = listOfNotNull(medication.name.value, strength, dose, frequency, archivedText.takeIf { archived })
        .joinToString(", ")
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.medication_action_edit), onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Row(
            Modifier.padding(12.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PillIcon(medication.appearance)
            Column(Modifier.weight(1f)) {
                Text(medication.name.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (strength != null) Text(strength, style = MaterialTheme.typography.bodyMedium)
                Text(dose, style = MaterialTheme.typography.bodyMedium)
                Text(frequency, style = MaterialTheme.typography.bodySmall)
                if (archived) {
                    Text(archivedText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
