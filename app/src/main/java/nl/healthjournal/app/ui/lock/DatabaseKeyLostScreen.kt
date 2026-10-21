package nl.healthjournal.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.backup.BackupHost
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.common.asString

/**
 * Shown instead of the app when the database encryption key is gone and the data cannot be read.
 * The way forward is to restore from an encrypted backup, or to delete the unreadable data; both need an explicit
 * confirmation. A restore replaces the unreadable data only after the backup has been opened and checked.
 */
@Composable
fun DatabaseKeyLostScreen(
    onDeleteData: () -> Unit,
    backupViewModel: BackupViewModel,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirming by remember { mutableStateOf(false) }

    BackupHost(backupViewModel, lostKey = true) { actions, state ->
        // A finished restore has put a fresh database in place; start over so the app opens on it.
        LaunchedEffect(state.message) { if (state.message != null) onRestart() }
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.key_lost_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = stringResource(R.string.key_lost_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                state.error?.let {
                    Text(
                        text = it.asString(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                Button(
                    onClick = actions.restore,
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.key_lost_restore_button))
                }
                OutlinedButton(
                    onClick = { confirming = true },
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.key_lost_delete_button), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.key_lost_confirm_title)) },
            text = { Text(stringResource(R.string.key_lost_confirm_body)) },
            confirmButton = {
                TextButton(onClick = onDeleteData) {
                    Text(
                        stringResource(R.string.key_lost_confirm_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.key_lost_confirm_cancel))
                }
            }
        )
    }
}
