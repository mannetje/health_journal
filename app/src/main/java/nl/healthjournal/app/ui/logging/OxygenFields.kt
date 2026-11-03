package nl.healthjournal.app.ui.logging

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.metrics.OxygenContext
import nl.healthjournal.domain.model.metrics.OxygenSite

@get:StringRes
val OxygenContext.labelRes: Int
    get() = when (this) {
        OxygenContext.AT_REST -> R.string.oxygen_context_at_rest
        OxygenContext.AFTER_ACTIVITY -> R.string.oxygen_context_after_activity
        OxygenContext.ASLEEP_OR_JUST_WOKE -> R.string.oxygen_context_asleep_or_just_woke
        OxygenContext.OTHER -> R.string.oxygen_context_other
    }

@get:StringRes
val OxygenSite.labelRes: Int
    get() = when (this) {
        OxygenSite.FINGER -> R.string.oxygen_site_finger
        OxygenSite.TOE -> R.string.oxygen_site_toe
        OxygenSite.EARLOBE -> R.string.oxygen_site_earlobe
    }

/**
 * The optional notes of an oxygen saturation entry, shared by the logging form and the edit dialog: pulse, the
 * situation, where it was measured and whether oxygen was given. Choosing a selected chip again clears it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OxygenOptionFields(
    pulse: String,
    onPulseChange: (String) -> Unit,
    context: OxygenContext?,
    onContextSelected: (OxygenContext) -> Unit,
    site: OxygenSite?,
    onSiteSelected: (OxygenSite) -> Unit,
    oxygenGiven: Boolean,
    onOxygenGivenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = pulse,
            onValueChange = onPulseChange,
            label = { Text(stringResource(R.string.log_oxygen_pulse_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(stringResource(R.string.log_oxygen_context_title), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OxygenContext.entries.forEach { option ->
                FilterChip(
                    selected = context == option,
                    onClick = { onContextSelected(option) },
                    label = { Text(stringResource(option.labelRes)) }
                )
            }
        }

        Text(stringResource(R.string.log_oxygen_site_title), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OxygenSite.entries.forEach { option ->
                FilterChip(
                    selected = site == option,
                    onClick = { onSiteSelected(option) },
                    label = { Text(stringResource(option.labelRes)) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.log_oxygen_given), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = oxygenGiven, onCheckedChange = onOxygenGivenChange)
        }
    }
}
