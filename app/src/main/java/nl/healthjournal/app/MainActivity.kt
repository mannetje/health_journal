package nl.healthjournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LogMetricScreen
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.profile.ProfileScreen
import nl.healthjournal.app.ui.profile.ProfileViewModel
import nl.healthjournal.app.ui.theme.HealthJournalTheme

enum class AppNavDestination {
    LOG,
    HISTORY,
    PROFILE
}

class MainActivity : ComponentActivity() {

    private val app by lazy { application as HealthJournalApp }

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
            app.recordActivityUseCase
        )
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        HistoryViewModel.Factory(
            app.profileRepository,
            app.getHealthHistoryUseCase,
            app.dataExportAdapter,
            app.dataImportAdapter
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val languagePreference = remember { LanguagePreference(this) }
            var currentLanguage by remember { mutableStateOf(languagePreference.language) }

            val baseContext = LocalContext.current
            val localizedContext = remember(currentLanguage) {
                baseContext.withAppLocale(currentLanguage)
            }

            // The localized context is not an Activity, so activity-result launchers (the CSV file picker)
            // can no longer find their registry through LocalContext; provide it explicitly.
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                HealthJournalTheme {
                    var currentTab by remember { mutableStateOf(AppNavDestination.LOG) }

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        when (currentTab) {
                                            AppNavDestination.LOG -> stringResource(R.string.nav_title_log)
                                            AppNavDestination.HISTORY -> stringResource(R.string.nav_title_history)
                                            AppNavDestination.PROFILE -> stringResource(R.string.nav_title_profile)
                                        }
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == AppNavDestination.LOG,
                                    onClick = { currentTab = AppNavDestination.LOG },
                                    label = { Text(stringResource(R.string.nav_tab_log)) },
                                    icon = { Text("📝") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == AppNavDestination.HISTORY,
                                    onClick = { currentTab = AppNavDestination.HISTORY },
                                    label = { Text(stringResource(R.string.nav_tab_history)) },
                                    icon = { Text("📊") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == AppNavDestination.PROFILE,
                                    onClick = { currentTab = AppNavDestination.PROFILE },
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
                            when (currentTab) {
                                AppNavDestination.LOG -> LogMetricScreen(viewModel = loggingViewModel)
                                AppNavDestination.HISTORY -> HistoryScreen(viewModel = historyViewModel)
                                AppNavDestination.PROFILE -> ProfileScreen(
                                    viewModel = profileViewModel,
                                    currentLanguage = currentLanguage,
                                    onLanguageChange = {
                                        currentLanguage = it
                                        languagePreference.language = it
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
