package nl.healthjournal.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import nl.healthjournal.app.reminder.ReminderIntents
import nl.healthjournal.app.lock.AppLockManager
import nl.healthjournal.app.lock.BiometricAuthHelper
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.MedicationNoticePreference
import nl.healthjournal.app.settings.SecureScreenPreference
import nl.healthjournal.app.settings.ThemePreference
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.backup.BackupEvent
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.lock.DatabaseKeyLostScreen
import nl.healthjournal.app.ui.lock.LockOverlay
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.migraine.MigraineLogViewModel
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.shell.AppSettingsState
import nl.healthjournal.app.ui.shell.PreferencesSettingsStore
import nl.healthjournal.app.ui.shell.ReminderPermissionState
import nl.healthjournal.app.ui.shell.ViewModelRefresher
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.app.ui.theme.HealthJournalTheme
import nl.healthjournal.data.local.DatabaseEncryptionStatus
import nl.healthjournal.data.local.DatabaseRecovery
import nl.healthjournal.data.local.EncryptionState

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val app by lazy { application as HealthJournalApp }

    // Owned by the Application: ViewModels outlive this Activity and must not capture it.
    private val unitPreference get() = app.unitPreference
    private val themePreference by lazy { ThemePreference(this) }
    private val noticePreference by lazy { MedicationNoticePreference(this) }
    private val secureScreenPreference by lazy { SecureScreenPreference(this) }
    // Owned by the Application, so a recreated Activity (rotation, language change) does not lock the app again.
    private val appLockManager: AppLockManager get() = app.appLock
    private val biometricAuthHelper by lazy {
        BiometricAuthHelper(this) {
            appLockManager.onAuthenticated()
        }
    }

    private val confirmDeviceCredentialLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (!biometricAuthHelper.runPendingAction()) appLockManager.onAuthenticated()
            }
        }

    private val backupViewModel: BackupViewModel by viewModels { app.backupViewModelFactory(contentResolver) }
    private val profileViewModel: ProfileViewModel by viewModels()
    private val loggingViewModel: LoggingViewModel by viewModels()
    private val migraineViewModel: MigraineLogViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val medicationViewModel: MedicationViewModel by viewModels()

    private val refresher by lazy {
        ViewModelRefresher.of(
            profileViewModel, loggingViewModel, migraineViewModel, historyViewModel, medicationViewModel,
            backupViewModel, app.reminders
        )
    }

    private suspend fun onBackupRestored(event: BackupEvent.Restored) {
        refresher.onRestored()
        // Language, region, theme and units are read when the screen is built.
        if (event.preferencesApplied) recreate()
    }

    /** Restore on the lost-key screen. A separate key: viewModels() would hand back the same instance as backupViewModel. */
    private val recoveryBackupViewModel: BackupViewModel by lazy {
        ViewModelProvider(this, app.recoveryBackupViewModelFactory(applicationContext))["recovery", BackupViewModel::class.java]
    }

    private fun restartApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(Intent.makeRestartActivityTask(launch.component))
        Runtime.getRuntime().exit(0)
    }

    /** Recovery for a lost database key: removes the unreadable data and restarts with a fresh, empty database. */
    private fun deleteUnreadableDataAndRestart() {
        DatabaseRecovery.deleteUnreadableData(this)
        restartApp()
    }

    fun applySecureScreen(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }


    private val shellViewModels by lazy {
        ShellViewModels(
            profileViewModel, loggingViewModel, migraineViewModel, historyViewModel, medicationViewModel,
            backupViewModel
        )
    }

    /** Set when a reminder notification was tapped; the composition opens the pillbox and clears it. */
    private var pillboxRequested by mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(ReminderIntents.EXTRA_OPEN_PILLBOX, false)) pillboxRequested = true
    }

    private val reminderPermissions by lazy {
        ReminderPermissionState(
            readNotificationsAllowed = { getSystemService(NotificationManager::class.java).areNotificationsEnabled() },
            readExactAlarmsAllowed = app.reminders::canScheduleExact,
            askedBefore = { app.reminderPreference.permissionAsked },
            markAsked = { app.reminderPreference.permissionAsked = true },
            needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { reminderPermissions.refresh() }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }


    /** Notifications may be stale after time spent outside the app (a dose taken elsewhere, a changed schedule). */
    override fun onResume() {
        super.onResume()
        appLockManager.onForegrounded()
        reminderPermissions.refresh()
        lifecycleScope.launch { app.reminders.changed() }
    }

    override fun onStop() {
        super.onStop()
        // A configuration change or recreate() is not leaving the app, so it must not start the lock timeout.
        if (!isChangingConfigurations) appLockManager.onBackgrounded()
    }

    /** Applies the chosen app language and region to the whole Activity, including dialogs and pickers. */
    override fun attachBaseContext(newBase: Context) {
        val preference = LanguagePreference(newBase)
        super.attachBaseContext(newBase.withAppLocale(preference.language, preference.region))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DatabaseEncryptionStatus.state == EncryptionState.KeyLost) {
            // The database key is gone: show the recovery screen only, nothing else touches the data.
            applySecureScreen(true)
            setContent {
                HealthJournalTheme(choice = themePreference.choice) {
                    DatabaseKeyLostScreen(
                        onDeleteData = ::deleteUnreadableDataAndRestart,
                        backupViewModel = recoveryBackupViewModel,
                        onRestart = ::restartApp
                    )
                }
            }
            return
        }
        // The app lock implies a secure window: without it the recent-apps thumbnail would show health data.
        applySecureScreen(secureScreenPreference.enabled || appLockManager.isEnabled)
        if (savedInstanceState == null && intent.getBooleanExtra(ReminderIntents.EXTRA_OPEN_PILLBOX, false)) {
            pillboxRequested = true
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                backupViewModel.events.collect { if (it is BackupEvent.Restored) onBackupRestored(it) }
            }
        }
        setContent {
            val settings = remember {
                AppSettingsState(
                    PreferencesSettingsStore(
                        LanguagePreference(this), unitPreference, themePreference, noticePreference,
                        app.reminderPreference, secureScreenPreference, appLockManager
                    )
                )
            }
            val lockState by appLockManager.lockState.collectAsStateWithLifecycle()
            val actions = remember {
                ShellActions(
                    recreate = ::recreate,
                    requestNotificationPermission = ::requestNotificationPermission,
                    reminderSettings = {
                        ReminderSettings(
                            settings, reminderPermissions,
                            onRemindersChanged = { lifecycleScope.launch { app.reminders.changed() } },
                            onOpenNotificationSettings = { openNotificationSettings() },
                            onOpenExactAlarmSettings = { openExactAlarmSettings() }
                        )
                    },
                    securitySettings = {
                        SecuritySettings(
                            settings,
                            appLockAvailable = appLockManager.isAvailable,
                            appLockEnabled = { appLockManager.isEnabled },
                            confirmWeakening = { biometricAuthHelper.confirm(it, confirmDeviceCredentialLauncher) },
                            applySecureScreen = ::applySecureScreen
                        )
                    }
                )
            }

            CompositionLocalProvider(LocalDisplayUnits provides settings.displayUnits) {
                HealthJournalTheme(choice = settings.theme) {
                    if (lockState.requiresAuthentication) {
                        LockOverlay(onUnlock = { biometricAuthHelper.authenticate(confirmDeviceCredentialLauncher) })
                    } else {
                        AppContent(
                            settings = settings,
                            reminderPermissions = reminderPermissions,
                            refresher = refresher,
                            viewModels = shellViewModels,
                            startOnMigraineTab = app.metricDisplayOrderPreference.order.value.first() == MetricSlot.MIGRAINE,
                            pillboxRequested = pillboxRequested,
                            onPillboxRequestHandled = { pillboxRequested = false },
                            actions = actions
                        )
                    }
                }
            }
        }
    }
}
