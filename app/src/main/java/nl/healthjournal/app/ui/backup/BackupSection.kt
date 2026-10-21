package nl.healthjournal.app.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.app.ui.theme.onSuccessContainerColor
import nl.healthjournal.app.ui.theme.successContainerColor

/** The "Backup" block of the Profile tab: create and restore an encrypted `.hjbackup`. */
@Composable
fun BackupSection(viewModel: BackupViewModel, modifier: Modifier = Modifier) {
    BackupHost(viewModel) { actions, state ->
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider()
            Text(stringResource(R.string.backup_section_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.backup_section_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.message?.let {
                Card(colors = CardDefaults.cardColors(containerColor = successContainerColor)) {
                    Text(it.asString(), color = onSuccessContainerColor, modifier = Modifier.padding(12.dp))
                }
            }
            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it.asString(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                }
            }
            Button(
                onClick = actions.create,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.backup_create_button)) }
            OutlinedButton(
                onClick = actions.restore,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.backup_restore_button)) }
        }
    }
}
