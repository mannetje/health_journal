package nl.healthjournal.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.ReminderPreference

/** What the reminder section needs to show; read again when the app returns to the foreground. */
data class ReminderSettingsState(
    val snoozeMinutes: Int,
    val showDetailsOnLockScreen: Boolean,
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean
)

/** Snooze length, lock-screen privacy and the hints for permissions and battery settings. */
@Composable
fun ReminderSettingsSection(
    state: ReminderSettingsState,
    onSnoozeChange: (Int) -> Unit,
    onShowDetailsChange: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reminder_settings_title), style = MaterialTheme.typography.titleMedium)

        if (!state.notificationsAllowed) {
            HintWithAction(
                text = stringResource(R.string.reminder_hint_notifications_off),
                actionLabel = stringResource(R.string.reminder_open_settings),
                onAction = onOpenNotificationSettings
            )
        }
        if (!state.exactAlarmsAllowed) {
            HintWithAction(
                text = stringResource(R.string.reminder_hint_exact_off),
                actionLabel = stringResource(R.string.reminder_open_settings),
                onAction = onOpenExactAlarmSettings
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.reminder_snooze_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = ReminderPreference.SNOOZE_OPTIONS
                options.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        selected = state.snoozeMinutes == minutes,
                        onClick = { onSnoozeChange(minutes) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(stringResource(R.string.reminder_snooze_minutes, minutes), maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder_lock_screen_label), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.reminder_lock_screen_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = state.showDetailsOnLockScreen, onCheckedChange = onShowDetailsChange)
        }

        Text(
            stringResource(R.string.reminder_hint_battery),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HintWithAction(text: String, actionLabel: String, onAction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(actionLabel)
        }
    }
}
