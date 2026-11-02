package nl.healthjournal.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.ui.backup.BackupSection
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.lock.EncryptionNotActiveBanner
import nl.healthjournal.app.ui.logging.LogMetricScreen
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.medication.MedicationNoticeDialog
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.medication.PillboxScreen
import nl.healthjournal.app.ui.migraine.MigraineCard
import nl.healthjournal.app.ui.migraine.MigraineLogScreen
import nl.healthjournal.app.ui.migraine.MigraineLogViewModel
import nl.healthjournal.app.ui.mindfulness.ExerciseGate
import nl.healthjournal.app.ui.mindfulness.LocalExerciseGate
import nl.healthjournal.app.ui.mindfulness.MindfulnessArea
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.app.ui.shell.AppNavigationState
import nl.healthjournal.app.ui.shell.AppSettingsState
import nl.healthjournal.app.ui.shell.ProfileRoute
import nl.healthjournal.app.ui.shell.ReminderPermissionState
import nl.healthjournal.app.ui.shell.ViewModelRefresher
import nl.healthjournal.data.local.DatabaseEncryptionStatus
import nl.healthjournal.data.local.EncryptionState

/** The ViewModels the shell hands to its screens. */
class ShellViewModels(
    val profile: ProfileViewModel,
    val logging: LoggingViewModel,
    val migraine: MigraineLogViewModel,
    val history: HistoryViewModel,
    val medication: MedicationViewModel,
    val backup: BackupViewModel
)

/** What only the Activity can do, passed in as functions so the content holds no Activity. */
class ShellActions(
    val recreate: () -> Unit,
    val requestNotificationPermission: () -> Unit,
    val reminderSettings: @Composable () -> Unit,
    val securitySettings: @Composable () -> Unit
)

/**
 * Everything shown once the app is unlocked: the bars, the tab or full-screen area in the middle, and the dialogs.
 * The Activity builds the state holders and the actions; this only arranges them.
 */
@Composable
fun AppContent(
    settings: AppSettingsState,
    reminderPermissions: ReminderPermissionState,
    refresher: ViewModelRefresher,
    viewModels: ShellViewModels,
    startOnMigraineTab: Boolean,
    pillboxRequested: Boolean,
    onPillboxRequestHandled: () -> Unit,
    actions: ShellActions
) {
    val navigation = rememberSaveable(saver = AppNavigationState.Saver) {
        AppNavigationState(logMigraineTab = startOnMigraineTab)
    }
    val migraineState by viewModels.migraine.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = navigation.showPillbox) { navigation.closePillbox() }
    BackHandler(enabled = navigation.migraineBackEnabled(migraineState.open)) { viewModels.migraine.onBack() }
    BackHandler(enabled = navigation.showMindfulness) { navigation.closeMindfulness() }

    var showNoticeFromProfile by remember { mutableStateOf(false) }
    val exerciseGate = remember { ExerciseGate() }
    // Ask once, with an explanation, when the first schedule exists and the pillbox is in view.
    val hasMedication = viewModels.medication.uiState.collectAsStateWithLifecycle().value.medications.isNotEmpty()
    LaunchedEffect(hasMedication, navigation.showPillbox) {
        reminderPermissions.onPillboxState(hasMedication, navigation.showPillbox)
    }
    LaunchedEffect(pillboxRequested) {
        if (pillboxRequested) {
            navigation.openPillboxOnToday()
            onPillboxRequestHandled()
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
            AppNavigationBar(selected = navigation.selectedTab, onSelect = navigation::openTab)
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (navigation.showPillbox) {
                PillboxScreen(
                    viewModel = viewModels.medication,
                    showNotice = !settings.medicationNoticeAccepted,
                    openOnToday = navigation.openPillboxOnToday,
                    onOpenedOnToday = navigation::onOpenedOnToday,
                    onNoticeAccepted = settings::acceptMedicationNotice
                )
            } else if (navigation.showMindfulness) {
                CompositionLocalProvider(LocalExerciseGate provides exerciseGate) { MindfulnessArea() }
            } else when (navigation.currentTab) {
                AppNavDestination.LOG -> LogMetricScreen(
                    viewModel = viewModels.logging,
                    onGlucoseUnitSelected = { settings.changeGlucoseUnit(GlucoseUnitChoice.from(it)) },
                    migraineTabSelected = navigation.logMigraineTab,
                    onMigraineTabSelected = { navigation.logMigraineTab = it },
                    migraineContent = {
                        if (migraineState.open) {
                            MigraineLogScreen(viewModel = viewModels.migraine)
                        } else {
                            MigraineCard(state = migraineState, onLog = viewModels.migraine::startNew)
                        }
                    }
                )
                AppNavDestination.HISTORY ->
                    HistoryScreen(viewModel = viewModels.history, migraineViewModel = viewModels.migraine)
                AppNavDestination.PROFILE -> ProfileRoute(
                    viewModel = viewModels.profile,
                    settings = settings,
                    onLocaleChanged = actions.recreate,
                    onShowMedicationNotice = { showNoticeFromProfile = true },
                    reminderSettings = actions.reminderSettings,
                    securitySettings = actions.securitySettings,
                    backupSettings = { BackupSection(viewModel = viewModels.backup) },
                    versionName = BuildConfig.VERSION_NAME,
                    changelogAnchor = BuildConfig.CHANGELOG_ANCHOR
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
                actions.requestNotificationPermission()
            },
            onLater = reminderPermissions::answered
        )
    }
}
