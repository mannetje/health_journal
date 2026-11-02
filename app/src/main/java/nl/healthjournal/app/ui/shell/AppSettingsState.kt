package nl.healthjournal.app.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import nl.healthjournal.app.lock.AppLockManager
import nl.healthjournal.app.settings.AppLanguage
import nl.healthjournal.app.settings.AppRegion
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.LanguagePreference
import nl.healthjournal.app.settings.MedicationNoticePreference
import nl.healthjournal.app.settings.ReminderPreference
import nl.healthjournal.app.settings.SecureScreenPreference
import nl.healthjournal.app.settings.ThemeChoice
import nl.healthjournal.app.settings.ThemePreference
import nl.healthjournal.app.settings.UnitPreference
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.app.settings.resolveDisplayUnits
import nl.healthjournal.domain.model.lock.LockTimeout
import java.util.Locale

/** The saved settings the shell shows, read and written one value at a time. */
interface SettingsStore {
    var language: AppLanguage
    var region: AppRegion
    var unitSystem: UnitSystemChoice
    var glucoseUnit: GlucoseUnitChoice
    var theme: ThemeChoice
    var medicationNoticeAccepted: Boolean
    var snoozeMinutes: Int
    var showDetailsOnLockScreen: Boolean
    var secureScreen: Boolean
    var appLockEnabled: Boolean
    var lockTimeout: LockTimeout
}

/** The store over the app's real preferences and the lock manager. */
class PreferencesSettingsStore(
    private val languagePreference: LanguagePreference,
    private val unitPreference: UnitPreference,
    private val themePreference: ThemePreference,
    private val noticePreference: MedicationNoticePreference,
    private val reminderPreference: ReminderPreference,
    private val secureScreenPreference: SecureScreenPreference,
    private val appLockManager: AppLockManager
) : SettingsStore {
    override var language by languagePreference::language
    override var region by languagePreference::region
    override var unitSystem by unitPreference::system
    override var glucoseUnit by unitPreference::glucose
    override var theme by themePreference::choice
    override var medicationNoticeAccepted by noticePreference::accepted
    override var snoozeMinutes by reminderPreference::snoozeMinutes
    override var showDetailsOnLockScreen by reminderPreference::showDetailsOnLockScreen
    override var secureScreen by secureScreenPreference::enabled
    override var appLockEnabled by appLockManager::isEnabled
    override var lockTimeout by appLockManager::timeout
}

/**
 * The settings the shell shows, kept as observable state and written through to the store on every change. Holds no
 * Activity: what needs one (recreating after a language change, the window flag, asking for the device credential)
 * stays in the caller, which calls these setters once it is allowed to.
 */
class AppSettingsState(private val store: SettingsStore) {
    var language by mutableStateOf(store.language)
        private set
    var region by mutableStateOf(store.region)
        private set
    var unitSystem by mutableStateOf(store.unitSystem)
        private set
    var glucoseUnit by mutableStateOf(store.glucoseUnit)
        private set
    var theme by mutableStateOf(store.theme)
        private set
    var medicationNoticeAccepted by mutableStateOf(store.medicationNoticeAccepted)
        private set
    var snoozeMinutes by mutableStateOf(store.snoozeMinutes)
        private set
    var showDetailsOnLockScreen by mutableStateOf(store.showDetailsOnLockScreen)
        private set
    var secureScreen by mutableStateOf(store.secureScreen)
        private set
    var appLockEnabled by mutableStateOf(store.appLockEnabled)
        private set
    var lockTimeout by mutableStateOf(store.lockTimeout)
        private set

    /** Resolved against the app locale (Locale.getDefault), so the Regional formats setting drives the default. */
    val displayUnits: DisplayUnits get() = resolveDisplayUnits(unitSystem, glucoseUnit, Locale.getDefault())

    fun changeLanguage(value: AppLanguage) {
        language = value
        store.language = value
    }

    fun changeRegion(value: AppRegion) {
        region = value
        store.region = value
    }

    fun changeUnitSystem(value: UnitSystemChoice) {
        unitSystem = value
        store.unitSystem = value
    }

    fun changeGlucoseUnit(value: GlucoseUnitChoice) {
        glucoseUnit = value
        store.glucoseUnit = value
    }

    fun changeTheme(value: ThemeChoice) {
        theme = value
        store.theme = value
    }

    fun acceptMedicationNotice() {
        medicationNoticeAccepted = true
        store.medicationNoticeAccepted = true
    }

    fun changeSnoozeMinutes(value: Int) {
        snoozeMinutes = value
        store.snoozeMinutes = value
    }

    fun changeShowDetailsOnLockScreen(value: Boolean) {
        showDetailsOnLockScreen = value
        store.showDetailsOnLockScreen = value
    }

    fun changeSecureScreen(value: Boolean) {
        secureScreen = value
        store.secureScreen = value
    }

    fun changeAppLockEnabled(value: Boolean) {
        appLockEnabled = value
        store.appLockEnabled = value
    }

    fun changeLockTimeout(value: LockTimeout) {
        lockTimeout = value
        store.lockTimeout = value
    }
}
