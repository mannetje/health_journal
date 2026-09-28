package nl.healthjournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            HealthJournalTheme {
                var currentTab by remember { mutableStateOf(AppNavDestination.LOG) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (currentTab) {
                                        AppNavDestination.LOG -> "Health Journal - Log"
                                        AppNavDestination.HISTORY -> "Health Journal - History"
                                        AppNavDestination.PROFILE -> "Health Journal - Profile"
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
                                label = { Text("Log") },
                                icon = { Text("📝") }
                            )
                            NavigationBarItem(
                                selected = currentTab == AppNavDestination.HISTORY,
                                onClick = { currentTab = AppNavDestination.HISTORY },
                                label = { Text("History") },
                                icon = { Text("📊") }
                            )
                            NavigationBarItem(
                                selected = currentTab == AppNavDestination.PROFILE,
                                onClick = { currentTab = AppNavDestination.PROFILE },
                                label = { Text("Profile") },
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
                            AppNavDestination.PROFILE -> ProfileScreen(viewModel = profileViewModel)
                        }
                    }
                }
            }
        }
    }
}
