package nl.healthjournal.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.asString

/** What the screen can ask the backup flow to do. */
class BackupActions(val create: () -> Unit, val restore: () -> Unit)

/**
 * Registers the file pickers and shows the passphrase and confirmation dialogs of the backup flow, then lets
 * [content] place the buttons. Shared by the Profile tab and the lost-key screen ([lostKey] changes the
 * wording of the confirmation: the unreadable data is replaced instead of merged into).
 */
@Composable
fun BackupHost(
    viewModel: BackupViewModel,
    lostKey: Boolean = false,
    content: @Composable (BackupActions, BackupUiState) -> Unit
) {
    val state by viewModel.state.collectAsState()

    // No storage permission: the system picker hands over exactly the file the user chose.
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { viewModel.onRestoreFile(it) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        viewModel.onSaveTarget(it)
    }
    LaunchedEffect(state.saveSuggestion) {
        state.saveSuggestion?.let { saveLauncher.launch(it) }
    }

    content(BackupActions(create = viewModel::startCreate, restore = { openLauncher.launch(arrayOf("*/*")) }), state)

    when (state.step) {
        BackupStep.CreatePassphrase -> PassphraseDialog(
            title = stringResource(R.string.backup_create_title),
            confirmLabel = stringResource(R.string.backup_create_confirm),
            repeat = true,
            state = state,
            onSubmit = { pass, again -> viewModel.submitCreate(pass, again) },
            onCancel = viewModel::cancel
        )
        BackupStep.RestorePassphrase -> PassphraseDialog(
            title = stringResource(R.string.backup_restore_title),
            confirmLabel = stringResource(R.string.backup_open_button),
            repeat = false,
            state = state,
            onSubmit = { pass, _ -> viewModel.submitRestorePassphrase(pass) },
            onCancel = viewModel::cancel
        )
        BackupStep.RestoreConfirm -> state.preview?.let { preview ->
            RestoreConfirmDialog(
                preview = preview,
                lostKey = lostKey,
                busy = state.busy,
                onConfirm = viewModel::confirmRestore,
                onCancel = viewModel::cancel
            )
        }
        BackupStep.Idle -> Unit
    }
}

@Composable
private fun PassphraseDialog(
    title: String,
    confirmLabel: String,
    repeat: Boolean,
    state: BackupUiState,
    onSubmit: (CharArray, CharArray) -> Unit,
    onCancel: () -> Unit
) {
    // Plain remember, not rememberSaveable: the passphrase must never end up in saved instance state.
    var passphrase by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val fieldOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

    AlertDialog(
        onDismissRequest = { if (!state.busy) onCancel() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (repeat) {
                    Text(stringResource(R.string.backup_passphrase_warning), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(stringResource(R.string.backup_restore_prompt), style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text(stringResource(R.string.backup_passphrase_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = fieldOptions,
                    singleLine = true,
                    enabled = !state.busy,
                    isError = state.passphraseError != null && !repeat,
                    modifier = Modifier.fillMaxWidth()
                )
                if (repeat) {
                    OutlinedTextField(
                        value = again,
                        onValueChange = { again = it },
                        label = { Text(stringResource(R.string.backup_passphrase_confirm_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = fieldOptions,
                        singleLine = true,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // A neutral tip, not a strength meter: no score and no colours.
                    Text(
                        stringResource(R.string.backup_passphrase_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.passphraseError?.let {
                    Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (state.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.busy && passphrase.isNotEmpty(),
                onClick = { onSubmit(passphrase.toCharArray(), again.toCharArray()) }
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(enabled = !state.busy, onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun RestoreConfirmDialog(
    preview: RestorePreview,
    lostKey: Boolean,
    busy: Boolean,
    onConfirm: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var applySettings by remember { mutableStateOf(preview.hasPreferences) }
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.backup_restore_summary,
                        preview.profiles, preview.entries, preview.medications, preview.intakes
                    )
                )
                Text(
                    stringResource(if (lostKey) R.string.backup_restore_lostkey_note else R.string.backup_restore_merge_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (preview.unreadableItems > 0) {
                    Text(
                        stringResource(R.string.backup_restore_unreadable_note, preview.unreadableItems),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (preview.hasPreferences) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applySettings, onCheckedChange = { applySettings = it }, enabled = !busy)
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(stringResource(R.string.backup_restore_settings))
                            Text(
                                stringResource(R.string.backup_restore_settings_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { onConfirm(applySettings) }) {
                Text(stringResource(R.string.backup_restore_confirm))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
