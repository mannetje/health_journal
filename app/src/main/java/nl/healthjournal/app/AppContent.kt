package nl.healthjournal.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.ui.backup.BackupSection
import nl.healthjournal.app.ui.backup.BackupViewModel
import nl.healthjournal.app.ui.common.AdaptiveLayout
import nl.healthjournal.app.ui.common.NavigationLayout
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.lock.EncryptionNotActiveBanner
import nl.healthjournal.app.ui.logging.LogMetricScreen
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.medication.MedicationNoticeDialog
import nl.healthjournal.app.ui.medication.MedicationViewModel
import nl.healthjournal.app.ui.medication.PillboxScreen
import nl.healthjournal.app.ui.migraine.MigraineCard
import nl.healthjournal.app.ui.fluid.FluidLogScreen
import nl.healthjournal.app.ui.fluid.FluidLogViewModel
import nl.healthjournal.app.settings.MetricSlot
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
    val fluid: FluidLogViewModel,
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
    startOnCustomSlot: MetricSlot?,
    pillboxRequested: Boolean,
    onPillboxRequestHandled: () -> Unit,
    actions: ShellActions
) {
    val navigation = rememberSaveable(saver = AppNavigationState.Saver) {
        AppNavigationState(logCustomSlot = startOnCustomSlot)
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

    val configuration = LocalConfiguration.current
    val navigationLayout = AdaptiveLayout.navigationLayout(configuration.screenWidthDp, configuration.screenHeightDp)
    val historyTwoPane = AdaptiveLayout.historyTwoPane(configuration.screenWidthDp)
    val contentMaxWidthDp = AdaptiveLayout.contentMaxWidthDp(
        historyTwoPane && navigation.currentTab == AppNavDestination.HISTORY &&
            !navigation.showPillbox && !navigation.showMindfulness
    )

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
            if (navigationLayout == NavigationLayout.BAR) {
                AppNavigationBar(selected = navigation.selectedTab, onSelect = navigation::openTab)
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (navigationLayout == NavigationLayout.RAIL) {
                AppNavigationRail(selected = navigation.selectedTab, onSelect = navigation::openTab)
            }
            // Centred and capped on wide windows so lines, chips and fields stay readable.
            Surface(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = contentMaxWidthDp.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
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
                                customSlot = navigation.logCustomSlot,
                                onCustomSlotSelected = { navigation.logCustomSlot = it },
                                customContent = { slot ->
                                    if (slot == MetricSlot.FLUID) {
                                        FluidLogScreen(viewModel = viewModels.fluid)
                                    } else if (migraineState.open) {
                                        MigraineLogScreen(viewModel = viewModels.migraine)
                                    } else {
                                        MigraineCard(state = migraineState, onLog = viewModels.migraine::startNew)
                                    }
                                }
                            )
                            AppNavDestination.HISTORY ->
                                HistoryScreen(viewModel = viewModels.history, migraineViewModel = viewModels.migraine, twoPane = historyTwoPane)
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
