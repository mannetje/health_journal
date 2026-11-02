package nl.healthjournal.app

import androidx.compose.runtime.Composable
import nl.healthjournal.app.ui.profile.ReminderSettingsSection
import nl.healthjournal.app.ui.profile.ReminderSettingsState
import nl.healthjournal.app.ui.profile.SecuritySettingsSection
import nl.healthjournal.app.ui.profile.SecuritySettingsState
import nl.healthjournal.app.ui.shell.AppSettingsState
import nl.healthjournal.app.ui.shell.ReminderPermissionState
import nl.healthjournal.data.local.DatabaseEncryptionStatus
import nl.healthjournal.data.local.EncryptionState

/** The Profile reminder section; what only the Activity can do arrives as functions. */
@Composable
internal fun ReminderSettings(
    settings: AppSettingsState,
    permissions: ReminderPermissionState,
    onRemindersChanged: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    ReminderSettingsSection(
        state = ReminderSettingsState(
            snoozeMinutes = settings.snoozeMinutes,
            showDetailsOnLockScreen = settings.showDetailsOnLockScreen,
            notificationsAllowed = permissions.notificationsAllowed,
            exactAlarmsAllowed = permissions.exactAlarmsAllowed
        ),
        onSnoozeChange = settings::changeSnoozeMinutes,
        onShowDetailsChange = {
            settings.changeShowDetailsOnLockScreen(it)
            onRemindersChanged()
        },
        onOpenNotificationSettings = onOpenNotificationSettings,
        onOpenExactAlarmSettings = onOpenExactAlarmSettings
    )
}

/**
 * The Profile security section. [confirmWeakening] runs its action only after the device credential is confirmed, so a
 * borrowed unlocked phone cannot switch the lock off or lengthen its timeout. [applySecureScreen] gets the window flag.
 */
@Composable
internal fun SecuritySettings(
    settings: AppSettingsState,
    appLockAvailable: Boolean,
    appLockEnabled: () -> Boolean,
    confirmWeakening: (apply: () -> Unit) -> Unit,
    applySecureScreen: (Boolean) -> Unit
) {
    SecuritySettingsSection(
        state = SecuritySettingsState(
            appLockEnabled = settings.appLockEnabled,
            appLockAvailable = appLockAvailable,
            timeout = settings.lockTimeout,
            secureScreenEnabled = settings.secureScreen,
            encryptionActive = DatabaseEncryptionStatus.state == EncryptionState.Active
        ),
        onAppLockChange = { enable ->
            val apply = {
                settings.changeAppLockEnabled(enable)
                applySecureScreen(settings.secureScreen || appLockEnabled())
            }
            if (enable) apply() else confirmWeakening(apply)
        },
        onTimeoutChange = { newTimeout ->
            val apply = { settings.changeLockTimeout(newTimeout) }
            if (newTimeout.seconds > settings.lockTimeout.seconds) confirmWeakening(apply) else apply()
        },
        onSecureScreenChange = {
            settings.changeSecureScreen(it)
            applySecureScreen(it || appLockEnabled())
        }
    )
}
