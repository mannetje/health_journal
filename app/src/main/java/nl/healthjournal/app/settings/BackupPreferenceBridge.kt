package nl.healthjournal.app.settings

import android.content.Context
import nl.healthjournal.domain.model.backup.BackupPreferences

/**
 * Maps the display preferences to and from the backup document (`system` means follow the device).
 * Only display preferences are handled here: the app lock, secure screen, lock-screen details, the medication
 * notice and the permission flag are never read into a backup and never written from one.
 */
class BackupPreferenceBridge(context: Context) {
    private val language = LanguagePreference(context)
    private val units = UnitPreference(context)
    private val theme = ThemePreference(context)
    private val reminders = ReminderPreference(context)

    fun collect(): BackupPreferences = BackupPreferences(
        language = language.language.tag ?: SYSTEM,
        region = language.region.tag ?: SYSTEM,
        unitSystem = units.system.tag ?: SYSTEM,
        glucoseUnit = units.glucose.tag ?: SYSTEM,
        theme = theme.choice.tag ?: SYSTEM,
        reminderSnoozeMinutes = reminders.snoozeMinutes
    )

    fun apply(preferences: BackupPreferences) {
        language.language = AppLanguage.fromTag(preferences.language.takeIf { it != SYSTEM })
        language.region = AppRegion.fromTag(preferences.region.takeIf { it != SYSTEM })
        units.system = UnitSystemChoice.fromTag(preferences.unitSystem.takeIf { it != SYSTEM })
        units.glucose = GlucoseUnitChoice.fromTag(preferences.glucoseUnit.takeIf { it != SYSTEM })
        theme.choice = ThemeChoice.fromTag(preferences.theme.takeIf { it != SYSTEM })
        reminders.snoozeMinutes = preferences.reminderSnoozeMinutes
    }

    private companion object {
        const val SYSTEM = "system"
    }
}
