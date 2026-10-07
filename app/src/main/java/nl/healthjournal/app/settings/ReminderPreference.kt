package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences

/** Reminder settings: how long Snooze waits, and whether the lock screen shows the medication names. */
class ReminderPreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var snoozeMinutes: Int
        get() = prefs.getInt(KEY_SNOOZE, DEFAULT_SNOOZE).takeIf { it in SNOOZE_OPTIONS } ?: DEFAULT_SNOOZE
        set(value) = prefs.edit().putInt(KEY_SNOOZE, value.takeIf { it in SNOOZE_OPTIONS } ?: DEFAULT_SNOOZE).apply()

    /** Off by default: the lock screen shows only a generic text. */
    var showDetailsOnLockScreen: Boolean
        get() = prefs.getBoolean(KEY_DETAILS, false)
        set(value) = prefs.edit().putBoolean(KEY_DETAILS, value).apply()

    /** The notification permission is asked once, at the first saved schedule. */
    var permissionAsked: Boolean
        get() = prefs.getBoolean(KEY_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED, value).apply()

    companion object {
        val SNOOZE_OPTIONS = listOf(10, 30, 60)
        const val DEFAULT_SNOOZE = 10
        private const val PREFS_NAME = "reminder_prefs"
        private const val KEY_SNOOZE = "snooze_minutes"
        private const val KEY_DETAILS = "show_details_on_lock_screen"
        private const val KEY_ASKED = "permission_asked"
    }
}
