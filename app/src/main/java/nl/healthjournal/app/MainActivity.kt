package nl.healthjournal.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.app.settings.withAppLocale
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.history.HistoryScreen
import nl.healthjournal.app.ui.history.HistoryViewModel
import nl.healthjournal.app.ui.logging.LogMetricScreen
import nl.healthjournal.app.ui.logging.LoggingViewModel
import nl.healthjournal.app.ui.profile.ProfileScreen
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

    /** Applies the chosen app language and region to the whole Activity, including dialogs and pickers. */
    override fun attachBaseContext(newBase: Context) {
        val preference = LanguagePreference(newBase)
        super.attachBaseContext(newBase.withAppLocale(preference.language, preference.region))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val languagePreference = remember { LanguagePreference(this) }
            var currentLanguage by remember { mutableStateOf(languagePreference.language) }
            var currentRegion by remember { mutableStateOf(languagePreference.region) }
            var unitSystemChoice by remember { mutableStateOf(unitPreference.system) }
            var glucoseUnitChoice by remember { mutableStateOf(unitPreference.glucose) }
            // Resolved against the app locale (Locale.getDefault), so the Regional formats setting drives the default.
            val displayUnits = remember(unitSystemChoice, glucoseUnitChoice) { unitPreference.resolve() }

            CompositionLocalProvider(LocalDisplayUnits provides displayUnits) {
            HealthJournalTheme {
                var currentTab by rememberSaveable { mutableStateOf(AppNavDestination.LOG) }

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
                            actions = {
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
                                currentRegion = currentRegion,
                                onRegionChange = {
                                    currentRegion = it
                                    languagePreference.region = it
                                    recreate()
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
