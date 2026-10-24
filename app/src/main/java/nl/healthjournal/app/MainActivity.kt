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
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.healthjournal.app.reminder.ReminderIntents
import nl.healthjournal.app.lock.AppLockManager
import nl.healthjournal.app.lock.BiometricAuthHelper
import nl.healthjournal.app.settings.AppLockPreference
import nl.healthjournal.app.settings.BackupPreferenceBridge
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.MedicationNoticePreference
import nl.healthjournal.app.settings.SecureScreenPreference
import nl.healthjournal.app.settings.ThemePreference
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.backup.BackupSection
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.backup.ContentResolverBackupFileIo
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.lock.DatabaseKeyLostScreen
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
import nl.healthjournal.domain.usecase.RestoreBackupUseCase
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.app.ui.profile.ReminderSettingsSection
import nl.healthjournal.app.ui.profile.ReminderSettingsState
import nl.healthjournal.app.ui.profile.SecuritySettingsSection
import nl.healthjournal.app.ui.profile.SecuritySettingsState
import nl.healthjournal.app.ui.theme.BrandNavy
import nl.healthjournal.app.ui.theme.HealthJournalTheme
import nl.healthjournal.data.backup.FreshDatabaseBackupSnapshot
import nl.healthjournal.data.local.DatabaseEncryptionStatus
import nl.healthjournal.data.local.DatabaseRecovery
import nl.healthjournal.data.local.EncryptionState

enum class AppNavDestination {
    LOG,
    HISTORY,
    PROFILE
}

class MainActivity : FragmentActivity() {

    private val app by lazy { application as HealthJournalApp }

    private val unitPreference by lazy { UnitPreference(this) }
    private val themePreference by lazy { ThemePreference(this) }
    private val noticePreference by lazy { MedicationNoticePreference(this) }
    private val secureScreenPreference by lazy { SecureScreenPreference(this) }
    private val appLockPreference by lazy { AppLockPreference(this) }
    val appLockManager by lazy { AppLockManager(this, appLockPreference) }
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

    private val backupPreferences by lazy { BackupPreferenceBridge(this) }

    private val backupViewModel: BackupViewModel by viewModels {
        BackupViewModel.Factory(
            create = app.createBackupUseCase,
            restore = app.restoreBackupUseCase,
            io = ContentResolverBackupFileIo(contentResolver),
            preferences = { backupPreferences.collect() },
            applyPreferences = {
                backupPreferences.apply(it)
                // Language, region, theme and units are read when the screen is built.
                recreate()
            },
            appVersion = versionName,
            onRestored = {
                profileViewModel.loadProfile()
                loggingViewModel.loadProfile()
                migraineViewModel.loadProfile()
                historyViewModel.loadHistory()
                medicationViewModel.refresh()
                lifecycleScope.launch { app.reminders.changed() }
            }
        )
    }

    /** Restore on the lost-key screen: the merge deletes the unreadable data first, into a fresh database. */
    private val recoveryBackupViewModel: BackupViewModel by lazy {
        // A separate key: viewModels() would hand back the same instance as backupViewModel.
        ViewModelProvider(this, BackupViewModel.Factory(
            create = null,
            restore = RestoreBackupUseCase(FreshDatabaseBackupSnapshot(applicationContext), app.backupCodec),
            io = ContentResolverBackupFileIo(contentResolver),
            preferences = { null },
            applyPreferences = { backupPreferences.apply(it) },
            appVersion = versionName,
            onRestored = {}
        ))["recovery", BackupViewModel::class.java]
    }

    private val versionName: String
        get() = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

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
            app.createProfileUseCase
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
            units = { unitPreference.resolve() }
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
            app.entryUseCases
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

    private var notificationsAllowed by mutableStateOf(true)
    private var exactAlarmsAllowed by mutableStateOf(true)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshReminderState() }

    private fun refreshReminderState() {
        notificationsAllowed = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        exactAlarmsAllowed = app.reminders.canScheduleExact()
    }

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
        refreshReminderState()
        lifecycleScope.launch { app.reminders.changed() }
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onBackgrounded()
    }

    /** Applies the chosen app language and region to the whole Activity, including dialogs and pickers. */
    override fun attachBaseContext(newBase: Context) {
        val preference = LanguagePreference(newBase)
        super.attachBaseContext(newBase.withAppLocale(preference.language, preference.region))
    }

    @OptIn(ExperimentalMaterial3Api::class)
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

            val lockState by appLockManager.lockState.collectAsState()
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
                var currentTab by rememberSaveable { mutableStateOf(AppNavDestination.LOG) }
                // The pillbox is a separate screen over the tabs, so Back returns to the tab it was opened from.
                var showPillbox by rememberSaveable { mutableStateOf(false) }
                // Set by a tap on a reminder notification; the pillbox then shows Today whatever it showed before.
                var openPillboxOnToday by remember { mutableStateOf(false) }
                // Mindfulness is also a screen over the tabs; it and the pillbox are never open together.
                var showMindfulness by rememberSaveable { mutableStateOf(false) }
                // The migraine tab of the Log screen; hoisted so it survives the form opening and closing.
                var logMigraineTab by rememberSaveable { mutableStateOf(false) }
                val migraineState by migraineViewModel.uiState.collectAsState()
                BackHandler(enabled = showPillbox) { showPillbox = false }
                BackHandler(enabled = migraineState.open && (currentTab == AppNavDestination.LOG || currentTab == AppNavDestination.HISTORY)) { migraineViewModel.onBack() }
                BackHandler(enabled = showMindfulness) { showMindfulness = false }

                var snoozeMinutes by remember { mutableStateOf(app.reminderPreference.snoozeMinutes) }
                var showDetails by remember { mutableStateOf(app.reminderPreference.showDetailsOnLockScreen) }
                var showPermissionDialog by remember { mutableStateOf(false) }
                val exerciseGate = remember { ExerciseGate() }
                // Ask once, with an explanation, when the first schedule exists and the pillbox is in view.
                val hasMedication = medicationViewModel.uiState.collectAsState().value.medications.isNotEmpty()
                LaunchedEffect(hasMedication, showPillbox) {
                    if (hasMedication && showPillbox && !app.reminderPreference.permissionAsked &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed
                    ) {
                        showPermissionDialog = true
                    }
                }
                LaunchedEffect(pillboxRequested) {
                    if (pillboxRequested) {
                        showMindfulness = false
                        showPillbox = true
                        openPillboxOnToday = true
                        pillboxRequested = false
                    }
                }

                // Banners belong to the screen that raised them. Skip the first run so a banner survives the
                // Activity recreation caused by a language change (ADR 0015).
                var previousTab by remember { mutableStateOf(currentTab) }
                LaunchedEffect(currentTab) {
                    if (currentTab != previousTab) {
                        previousTab = currentTab
                        loggingViewModel.clearMessages()
                        migraineViewModel.clearMessages()
                        profileViewModel.clearMessages()
                        historyViewModel.clearMessages()
                        medicationViewModel.clearMessages()
                        backupViewModel.clearMessages()
                    }
                }
                // A profile created on the Profile tab must reach the migraine tab when the Log tab opens.
                LaunchedEffect(currentTab) {
                    if (currentTab == AppNavDestination.LOG) migraineViewModel.loadProfile()
                }
                LaunchedEffect(showPillbox) {
                    if (!showPillbox) medicationViewModel.clearMessages()
                }

                val pillboxDescription = stringResource(R.string.nav_pillbox_desc)
                val mindfulnessDescription = stringResource(R.string.nav_mindfulness_desc)
                val logoDescription = stringResource(R.string.nav_logo_desc)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    if (showPillbox) stringResource(R.string.nav_title_pillbox) else if (showMindfulness) stringResource(R.string.nav_title_mindfulness) else when (currentTab) {
                                        AppNavDestination.LOG -> stringResource(R.string.nav_title_log)
                                        AppNavDestination.HISTORY -> stringResource(R.string.nav_title_history)
                                        AppNavDestination.PROFILE -> stringResource(R.string.nav_title_profile)
                                    }
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showMindfulness = false; showPillbox = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        "💊",
                                        modifier = Modifier.semantics { contentDescription = pillboxDescription }
                                    )
                                }
                                IconButton(
                                    onClick = { showPillbox = false; showMindfulness = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        "🪷",
                                        modifier = Modifier.semantics { contentDescription = mindfulnessDescription }
                                    )
                                }
                                Image(
                                    painter = painterResource(R.drawable.logo_health_journal_on_navy),
                                    contentDescription = logoDescription,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .height(40.dp)
                                        .clickable(role = Role.Button) {
                                            showPillbox = false
                                            showMindfulness = false
                                            currentTab = AppNavDestination.LOG
                                        }
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = BrandNavy,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = !showPillbox && !showMindfulness && currentTab == AppNavDestination.LOG,
                                onClick = { showPillbox = false; showMindfulness = false; currentTab = AppNavDestination.LOG },
                                label = { Text(stringResource(R.string.nav_tab_log)) },
                                icon = { Text("📝") }
                            )
                            NavigationBarItem(
                                selected = !showPillbox && !showMindfulness && currentTab == AppNavDestination.HISTORY,
                                onClick = { showPillbox = false; showMindfulness = false; currentTab = AppNavDestination.HISTORY },
                                label = { Text(stringResource(R.string.nav_tab_history)) },
                                icon = { Text("📊") }
                            )
                            NavigationBarItem(
                                selected = !showPillbox && !showMindfulness && currentTab == AppNavDestination.PROFILE,
                                onClick = { showPillbox = false; showMindfulness = false; currentTab = AppNavDestination.PROFILE },
                                label = { Text(stringResource(R.string.nav_tab_profile)) },
                                icon = { Text("👤") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showPillbox) {
                            PillboxScreen(
                                viewModel = medicationViewModel,
                                showNotice = !noticeAccepted,
                                openOnToday = openPillboxOnToday,
                                onOpenedOnToday = { openPillboxOnToday = false },
                                onNoticeAccepted = {
                                    noticePreference.accepted = true
                                    noticeAccepted = true
                                }
                            )
                        } else if (showMindfulness) {
                            CompositionLocalProvider(LocalExerciseGate provides exerciseGate) { MindfulnessArea() }
                        } else when (currentTab) {
                            AppNavDestination.LOG -> LogMetricScreen(
                                viewModel = loggingViewModel,
                                onGlucoseUnitSelected = {
                                    val choice = GlucoseUnitChoice.from(it)
                                    glucoseUnitChoice = choice
                                    unitPreference.glucose = choice
                                },
                                migraineTabSelected = logMigraineTab,
                                onMigraineTabSelected = { logMigraineTab = it },
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
                                            notificationsAllowed = notificationsAllowed,
                                            exactAlarmsAllowed = exactAlarmsAllowed
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
                if (showPermissionDialog && !exerciseGate.active) {
                    AlertDialog(
                        onDismissRequest = {
                            showPermissionDialog = false
                            app.reminderPreference.permissionAsked = true
                        },
                        title = { Text(stringResource(R.string.reminder_permission_title)) },
                        text = { Text(stringResource(R.string.reminder_permission_body)) },
                        confirmButton = {
                            TextButton(onClick = {
                                showPermissionDialog = false
                                app.reminderPreference.permissionAsked = true
                                requestNotificationPermission()
                            }) { Text(stringResource(R.string.reminder_permission_allow)) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showPermissionDialog = false
                                app.reminderPreference.permissionAsked = true
                            }) { Text(stringResource(R.string.reminder_permission_later)) }
                        }
                    )
                }
                }
            }
            }
        }
    }
}
