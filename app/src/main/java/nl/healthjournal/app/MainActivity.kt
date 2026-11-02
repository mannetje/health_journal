package nl.healthjournal.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import nl.healthjournal.app.reminder.ReminderIntents
import nl.healthjournal.app.lock.AppLockManager
import nl.healthjournal.app.lock.BiometricAuthHelper
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.MedicationNoticePreference
import nl.healthjournal.app.settings.SecureScreenPreference
import nl.healthjournal.app.settings.ThemePreference
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.backup.BackupEvent
import nl.healthjournal.app.ui.backup.BackupSection
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.backup.ContentResolverBackupFileIo
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.lock.DatabaseKeyLostScreen
import nl.healthjournal.app.ui.lock.EncryptionNotActiveBanner
import nl.healthjournal.app.ui.lock.LockOverlay
import nl.healthjournal.app.ui.logging.LogMetricScreen
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.migraine.MigraineCard
import nl.healthjournal.app.ui.migraine.MigraineLogScreen
import nl.healthjournal.app.ui.migraine.MigraineLogViewModel
import nl.healthjournal.app.ui.medication.MedicationNoticeDialog
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.medication.PillboxScreen
import nl.healthjournal.app.ui.mindfulness.ExerciseGate
import nl.healthjournal.app.ui.mindfulness.LocalExerciseGate
import nl.healthjournal.app.ui.mindfulness.MindfulnessArea
import nl.healthjournal.app.ui.profile.ProfileScreen
import nl.healthjournal.app.ui.shell.AppNavigationState
import nl.healthjournal.app.ui.shell.ReminderPermissionState
import nl.healthjournal.app.ui.shell.ViewModelRefresher
import nl.healthjournal.domain.usecase.RestoreBackupUseCase
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.app.ui.profile.ReminderSettingsSection
import nl.healthjournal.app.ui.profile.ReminderSettingsState
import nl.healthjournal.app.ui.profile.SecuritySettingsSection
import nl.healthjournal.app.ui.profile.SecuritySettingsState
import nl.healthjournal.app.ui.theme.HealthJournalTheme
import nl.healthjournal.data.backup.FreshDatabaseBackupSnapshot
import nl.healthjournal.data.local.DatabaseEncryptionStatus
import nl.healthjournal.data.local.DatabaseRecovery
import nl.healthjournal.data.local.EncryptionState

class MainActivity : FragmentActivity() {

    private val app by lazy { application as HealthJournalApp }

    // Owned by the Application: ViewModels outlive this Activity and must not capture it.
    private val unitPreference get() = app.unitPreference
    private val backupPreferences get() = app.backupPreferences
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

    private val backupViewModel: BackupViewModel by viewModels {
        BackupViewModel.Factory(
            create = app.createBackupUseCase,
            restore = app.restoreBackupUseCase,
            io = ContentResolverBackupFileIo(applicationContext.contentResolver),
            preferences = { backupPreferences.collect() },
            applyPreferences = { backupPreferences.apply(it) },
            appVersion = BuildConfig.VERSION_NAME
        )
    }

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

    /** Restore on the lost-key screen: the merge deletes the unreadable data first, into a fresh database. */
    private val recoveryBackupViewModel: BackupViewModel by lazy {
        // A separate key: viewModels() would hand back the same instance as backupViewModel.
        ViewModelProvider(this, BackupViewModel.Factory(
            create = null,
            restore = RestoreBackupUseCase(FreshDatabaseBackupSnapshot(applicationContext), app.backupCodec),
            io = ContentResolverBackupFileIo(applicationContext.contentResolver),
            preferences = { null },
            applyPreferences = { backupPreferences.apply(it) },
            appVersion = BuildConfig.VERSION_NAME
        ))["recovery", BackupViewModel::class.java]
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

    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModel.Factory(
            app.profileRepository,
            app.createProfileUseCase,
            app.metricDisplayOrderPreference.order,
            app.metricDisplayOrderPreference::save
        )
    }

    private val loggingViewModel: LoggingViewModel by viewModels {
        LoggingViewModel.Factory(
            app.profileRepository,
            app.recordWeightUseCase,
            app.recordBloodPressureUseCase,
            app.recordGlucoseUseCase,
            app.recordActivityUseCase,
            app.recordWaistCircumferenceUseCase,
            app.healthLogRepository,
            units = { unitPreference.resolve() },
            metricOrder = app.metricDisplayOrderPreference.order
        )
    }

    private val migraineViewModel: MigraineLogViewModel by viewModels {
        MigraineLogViewModel.Factory(app.profileRepository, app.migraineRepository)
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModel.Factory(
            app.profileRepository,
            app.getHealthHistoryUseCase,
            app.migraineRepository,
            app.dataExportAdapter,
            app.dataImportAdapter,
            app.migraineExportAdapter,
            app.migraineImportAdapter,
            app.entryUseCases,
            app.metricDisplayOrderPreference.order
        )
    }

    private val medicationViewModel: MedicationViewModel by viewModels {
        MedicationViewModel.Factory(
            app.profileRepository,
            app.medicationRepository,
            app.medicationUseCases,
            app.reminders
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

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
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
            val languagePreference = remember { LanguagePreference(this) }
            var currentLanguage by remember { mutableStateOf(languagePreference.language) }
            var currentRegion by remember { mutableStateOf(languagePreference.region) }
            var unitSystemChoice by remember { mutableStateOf(unitPreference.system) }
            var glucoseUnitChoice by remember { mutableStateOf(unitPreference.glucose) }
            var themeChoice by remember { mutableStateOf(themePreference.choice) }
            var noticeAccepted by remember { mutableStateOf(noticePreference.accepted) }
            var showNoticeFromProfile by remember { mutableStateOf(false) }
            // Resolved against the app locale (Locale.getDefault), so the Regional formats setting drives the default.
            val displayUnits = remember(unitSystemChoice, glucoseUnitChoice) { unitPreference.resolve() }

            val lockState by appLockManager.lockState.collectAsStateWithLifecycle()
            var appLockEnabled by remember { mutableStateOf(appLockManager.isEnabled) }
            var lockTimeout by remember { mutableStateOf(appLockManager.timeout) }
            var secureScreenEnabled by remember { mutableStateOf(secureScreenPreference.enabled) }

            CompositionLocalProvider(LocalDisplayUnits provides displayUnits) {
            HealthJournalTheme(choice = themeChoice) {
                if (lockState.requiresAuthentication) {
                    LockOverlay(
                        onUnlock = {
                            biometricAuthHelper.authenticate(confirmDeviceCredentialLauncher)
                        }
                    )
                } else {
                val navigation = rememberSaveable(saver = AppNavigationState.Saver) {
                    AppNavigationState(
                        logMigraineTab = app.metricDisplayOrderPreference.order.value.first() == MetricSlot.MIGRAINE
                    )
                }
                val migraineState by migraineViewModel.uiState.collectAsStateWithLifecycle()
                BackHandler(enabled = navigation.showPillbox) { navigation.closePillbox() }
                BackHandler(enabled = navigation.migraineBackEnabled(migraineState.open)) { migraineViewModel.onBack() }
                BackHandler(enabled = navigation.showMindfulness) { navigation.closeMindfulness() }

                var snoozeMinutes by remember { mutableStateOf(app.reminderPreference.snoozeMinutes) }
                var showDetails by remember { mutableStateOf(app.reminderPreference.showDetailsOnLockScreen) }
                val exerciseGate = remember { ExerciseGate() }
                // Ask once, with an explanation, when the first schedule exists and the pillbox is in view.
                val hasMedication = medicationViewModel.uiState.collectAsStateWithLifecycle().value.medications.isNotEmpty()
                LaunchedEffect(hasMedication, navigation.showPillbox) {
                    reminderPermissions.onPillboxState(hasMedication, navigation.showPillbox)
                }
                LaunchedEffect(pillboxRequested) {
                    if (pillboxRequested) {
                        navigation.openPillboxOnToday()
                        pillboxRequested = false
                    }
                }

                LaunchedEffect(navigation.currentTab) { refresher.onTabShown(navigation.currentTab) }
                LaunchedEffect(navigation.showPillbox) {
                    if (!navigation.showPillbox) refresher.onPillboxClosed()
                }

                Scaffold(
                    topBar = {
                        Column {
                            AppTopBar(
                                title = stringResource(
                                    when {
                                        navigation.showPillbox -> R.string.nav_title_pillbox
                                        navigation.showMindfulness -> R.string.nav_title_mindfulness
                                        else -> navigation.currentTab.titleRes
                                    }
                                ),
                                onOpenPillbox = navigation::openPillbox,
                                onOpenMindfulness = navigation::openMindfulness,
                                onOpenLog = { navigation.openTab(AppNavDestination.LOG) }
                            )
                            if (DatabaseEncryptionStatus.state is EncryptionState.NotActive && !navigation.encryptionBannerDismissed) {
                                EncryptionNotActiveBanner(
                                    onOpenProfile = { navigation.openTab(AppNavDestination.PROFILE) },
                                    onDismiss = { navigation.encryptionBannerDismissed = true }
                                )
                            }
                        }
                    },
                    bottomBar = {
                        AppNavigationBar(
                            selected = navigation.selectedTab,
                            onSelect = navigation::openTab
                        )
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (navigation.showPillbox) {
                            PillboxScreen(
                                viewModel = medicationViewModel,
                                showNotice = !noticeAccepted,
                                openOnToday = navigation.openPillboxOnToday,
                                onOpenedOnToday = navigation::onOpenedOnToday,
                                onNoticeAccepted = {
                                    noticePreference.accepted = true
                                    noticeAccepted = true
                                }
                            )
                        } else if (navigation.showMindfulness) {
                            CompositionLocalProvider(LocalExerciseGate provides exerciseGate) { MindfulnessArea() }
                        } else when (navigation.currentTab) {
                            AppNavDestination.LOG -> LogMetricScreen(
                                viewModel = loggingViewModel,
                                onGlucoseUnitSelected = {
                                    val choice = GlucoseUnitChoice.from(it)
                                    glucoseUnitChoice = choice
                                    unitPreference.glucose = choice
                                },
                                migraineTabSelected = navigation.logMigraineTab,
                                onMigraineTabSelected = { navigation.logMigraineTab = it },
                                migraineContent = {
                                    if (migraineState.open) {
                                        MigraineLogScreen(viewModel = migraineViewModel)
                                    } else {
                                        MigraineCard(state = migraineState, onLog = migraineViewModel::startNew)
                                    }
                                }
                            )
                            AppNavDestination.HISTORY -> HistoryScreen(viewModel = historyViewModel, migraineViewModel = migraineViewModel)
                            AppNavDestination.PROFILE -> ProfileScreen(
                                viewModel = profileViewModel,
                                currentLanguage = currentLanguage,
                                onLanguageChange = {
                                    currentLanguage = it
                                    languagePreference.language = it
                                    recreate()
                                },
                                currentUnitSystem = unitSystemChoice,
                                onUnitSystemChange = {
                                    unitSystemChoice = it
                                    unitPreference.system = it
                                },
                                currentGlucoseUnit = glucoseUnitChoice,
                                onGlucoseUnitChange = {
                                    glucoseUnitChoice = it
                                    unitPreference.glucose = it
                                },
                                currentTheme = themeChoice,
                                onThemeChange = {
                                    themeChoice = it
                                    themePreference.choice = it
                                },
                                currentRegion = currentRegion,
                                onRegionChange = {
                                    currentRegion = it
                                    languagePreference.region = it
                                    recreate()
                                },
                                onShowMedicationNotice = { showNoticeFromProfile = true },
                                reminderSettings = {
                                    ReminderSettingsSection(
                                        state = ReminderSettingsState(
                                            snoozeMinutes = snoozeMinutes,
                                            showDetailsOnLockScreen = showDetails,
                                            notificationsAllowed = reminderPermissions.notificationsAllowed,
                                            exactAlarmsAllowed = reminderPermissions.exactAlarmsAllowed
                                        ),
                                        onSnoozeChange = {
                                            snoozeMinutes = it
                                            app.reminderPreference.snoozeMinutes = it
                                        },
                                        onShowDetailsChange = {
                                            showDetails = it
                                            app.reminderPreference.showDetailsOnLockScreen = it
                                            lifecycleScope.launch { app.reminders.changed() }
                                        },
                                        onOpenNotificationSettings = ::openNotificationSettings,
                                        onOpenExactAlarmSettings = ::openExactAlarmSettings
                                    )
                                },
                                backupSettings = { BackupSection(viewModel = backupViewModel) },
                                versionName = BuildConfig.VERSION_NAME,
                                changelogAnchor = BuildConfig.CHANGELOG_ANCHOR,
                                securitySettings = {
                                    SecuritySettingsSection(
                                        state = SecuritySettingsState(
                                            appLockEnabled = appLockEnabled,
                                            appLockAvailable = appLockManager.isAvailable,
                                            timeout = lockTimeout,
                                            secureScreenEnabled = secureScreenEnabled,
                                            encryptionActive = DatabaseEncryptionStatus.state == EncryptionState.Active
                                        ),
                                        onAppLockChange = { enable ->
                                            val apply = {
                                                appLockEnabled = enable
                                                appLockManager.isEnabled = enable
                                                applySecureScreen(secureScreenEnabled || appLockManager.isEnabled)
                                            }
                                            // Weakening the lock needs the device credential, so a borrowed unlocked phone cannot switch it off.
                                            if (enable) apply()
                                            else biometricAuthHelper.confirm(apply, confirmDeviceCredentialLauncher)
                                        },
                                        onTimeoutChange = { newTimeout ->
                                            val apply = {
                                                lockTimeout = newTimeout
                                                appLockManager.timeout = newTimeout
                                            }
                                            if (newTimeout.seconds > lockTimeout.seconds) {
                                                biometricAuthHelper.confirm(apply, confirmDeviceCredentialLauncher)
                                            } else {
                                                apply()
                                            }
                                        },
                                        onSecureScreenChange = {
                                            secureScreenEnabled = it
                                            secureScreenPreference.enabled = it
                                            applySecureScreen(it || appLockManager.isEnabled)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
                if (showNoticeFromProfile) {
                    MedicationNoticeDialog(
                        confirmLabel = stringResource(android.R.string.ok),
                        onConfirm = { showNoticeFromProfile = false }
                    )
                }
                if (reminderPermissions.showDialog && !exerciseGate.active) {
                    ReminderPermissionDialog(
                        onAllow = {
                            reminderPermissions.answered()
                            requestNotificationPermission()
                        },
                        onLater = reminderPermissions::answered
                    )
                }
                }
            }
            }
        }
    }
}
