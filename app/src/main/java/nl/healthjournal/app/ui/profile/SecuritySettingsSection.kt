package nl.healthjournal.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.lock.LockTimeout

data class SecuritySettingsState(
    val appLockEnabled: Boolean,
    val appLockAvailable: Boolean,
    val timeout: LockTimeout,
    val secureScreenEnabled: Boolean
)

@Composable
fun SecuritySettingsSection(
    state: SecuritySettingsState,
    onAppLockChange: (Boolean) -> Unit,
    onTimeoutChange: (LockTimeout) -> Unit,
    onSecureScreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider()
        Text(stringResource(R.string.security_section_title), style = MaterialTheme.typography.titleMedium)

        // App Lock Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(R.string.security_app_lock_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (state.appLockAvailable) {
                        stringResource(R.string.security_app_lock_desc)
                    } else {
                        stringResource(R.string.security_app_lock_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.appLockAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = state.appLockEnabled && state.appLockAvailable,
                onCheckedChange = onAppLockChange,
                enabled = state.appLockAvailable
            )
        }

        // Timeout choices (only visible when app lock is active)
        if (state.appLockEnabled && state.appLockAvailable) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.security_timeout_label), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(LockTimeout.IMMEDIATELY, LockTimeout.ONE_MINUTE, LockTimeout.FIVE_MINUTES)
                    val labels = listOf(
                        stringResource(R.string.security_timeout_immediately),
                        stringResource(R.string.security_timeout_one_minute),
                        stringResource(R.string.security_timeout_five_minutes)
                    )
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = state.timeout == option,
                            onClick = { onTimeoutChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) {
                            Text(labels[index], maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Secure Screen Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(R.string.security_secure_screen_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.security_secure_screen_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.secureScreenEnabled,
                onCheckedChange = onSecureScreenChange
            )
        }

        // Privacy and Backup Notice
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.privacy_backup_notice_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.privacy_backup_notice_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
