package nl.healthjournal.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.healthjournal.app.reminder.ReminderIntents
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.MedicationNoticePreference
import nl.healthjournal.app.settings.ThemePreference
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LogMetricScreen
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.medication.MedicationNoticeDialog
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.medication.PillboxScreen
import nl.healthjournal.app.ui.profile.ProfileScreen
import nl.healthjournal.app.ui.profile.ReminderSettingsSection
import nl.healthjournal.app.ui.profile.ReminderSettingsState
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.app.ui.theme.BrandNavy
import nl.healthjournal.app.ui.theme.HealthJournalTheme

enum class AppNavDestination {
    LOG,
    HISTORY,
    PROFILE
}

class MainActivity : ComponentActivity() {

    private val app by lazy { application as HealthJournalApp }

    private val unitPreference by lazy { UnitPreference(this) }
    private val themePreference by lazy { ThemePreference(this) }
    private val noticePreference by lazy { MedicationNoticePreference(this) }

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

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModel.Factory(
            app.profileRepository,
            app.getHealthHistoryUseCase,
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
        refreshReminderState()
        lifecycleScope.launch { app.reminders.changed() }
    }

    /** Applies the chosen app language and region to the whole Activity, including dialogs and pickers. */
    override fun attachBaseContext(newBase: Context) {
        val preference = LanguagePreference(newBase)
        super.attachBaseContext(newBase.withAppLocale(preference.language, preference.region))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            CompositionLocalProvider(LocalDisplayUnits provides displayUnits) {
            HealthJournalTheme(choice = themeChoice) {
                var currentTab by rememberSaveable { mutableStateOf(AppNavDestination.LOG) }
                // The pillbox is a separate screen over the tabs, so Back returns to the tab it was opened from.
                var showPillbox by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = showPillbox) { showPillbox = false }

                var snoozeMinutes by remember { mutableStateOf(app.reminderPreference.snoozeMinutes) }
                var showDetails by remember { mutableStateOf(app.reminderPreference.showDetailsOnLockScreen) }
                var showPermissionDialog by remember { mutableStateOf(false) }
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
                        showPillbox = true
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
                        profileViewModel.clearMessages()
                        historyViewModel.clearMessages()
                        medicationViewModel.clearMessages()
                    }
                }
                LaunchedEffect(showPillbox) {
                    if (!showPillbox) medicationViewModel.clearMessages()
                }

                val pillboxDescription = stringResource(R.string.nav_pillbox_desc)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    if (showPillbox) stringResource(R.string.nav_title_pillbox) else when (currentTab) {
                                        AppNavDestination.LOG -> stringResource(R.string.nav_title_log)
                                        AppNavDestination.HISTORY -> stringResource(R.string.nav_title_history)
                                        AppNavDestination.PROFILE -> stringResource(R.string.nav_title_profile)
                                    }
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { showPillbox = true },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        "💊",
                                        modifier = Modifier.semantics { contentDescription = pillboxDescription }
                                    )
                                }
                                Image(
                                    painter = painterResource(R.drawable.logo_health_journal_on_navy),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .height(40.dp)
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
                                selected = !showPillbox && currentTab == AppNavDestination.LOG,
                                onClick = { showPillbox = false; currentTab = AppNavDestination.LOG },
                                label = { Text(stringResource(R.string.nav_tab_log)) },
                                icon = { Text("📝") }
                            )
                            NavigationBarItem(
                                selected = !showPillbox && currentTab == AppNavDestination.HISTORY,
                                onClick = { showPillbox = false; currentTab = AppNavDestination.HISTORY },
                                label = { Text(stringResource(R.string.nav_tab_history)) },
                                icon = { Text("📊") }
                            )
                            NavigationBarItem(
                                selected = !showPillbox && currentTab == AppNavDestination.PROFILE,
                                onClick = { showPillbox = false; currentTab = AppNavDestination.PROFILE },
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
                                onNoticeAccepted = {
                                    noticePreference.accepted = true
                                    noticeAccepted = true
                                }
                            )
                        } else when (currentTab) {
                            AppNavDestination.LOG -> LogMetricScreen(
                                viewModel = loggingViewModel,
                                onGlucoseUnitSelected = {
                                    val choice = GlucoseUnitChoice.from(it)
                                    glucoseUnitChoice = choice
                                    unitPreference.glucose = choice
                                }
                            )
                            AppNavDestination.HISTORY -> HistoryScreen(viewModel = historyViewModel)
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
                if (showPermissionDialog) {
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
