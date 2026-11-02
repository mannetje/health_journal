package nl.healthjournal.app.ui.shell

import androidx.compose.runtime.Composable
import nl.healthjournal.app.ui.profile.ProfileScreen
import nl.healthjournal.app.ui.profile.ProfileViewModel

/**
 * The Profile tab wired to the app settings. A language or region change is saved first and then reported through
 * [onLocaleChanged], because only the Activity can recreate itself to apply it.
 */
@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    settings: AppSettingsState,
    onLocaleChanged: () -> Unit,
    onShowMedicationNotice: () -> Unit,
    reminderSettings: @Composable () -> Unit,
    securitySettings: @Composable () -> Unit,
    backupSettings: @Composable () -> Unit,
    versionName: String,
    changelogAnchor: String
) {
    ProfileScreen(
        viewModel = viewModel,
        currentLanguage = settings.language,
        onLanguageChange = {
            settings.changeLanguage(it)
            onLocaleChanged()
        },
        currentUnitSystem = settings.unitSystem,
        onUnitSystemChange = settings::changeUnitSystem,
        currentGlucoseUnit = settings.glucoseUnit,
        onGlucoseUnitChange = settings::changeGlucoseUnit,
        currentTheme = settings.theme,
        onThemeChange = settings::changeTheme,
        currentRegion = settings.region,
        onRegionChange = {
            settings.changeRegion(it)
            onLocaleChanged()
        },
        onShowMedicationNotice = onShowMedicationNotice,
        reminderSettings = reminderSettings,
        securitySettings = securitySettings,
        backupSettings = backupSettings,
        versionName = versionName,
        changelogAnchor = changelogAnchor
    )
}
