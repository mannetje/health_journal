package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences

/** Remembers that the user has seen the pillbox notice, so it only shows on the first open. */
class MedicationNoticePreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accepted: Boolean
        get() = prefs.getBoolean(KEY_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_ACCEPTED, value).apply()

    companion object {
        private const val PREFS_NAME = "medication_prefs"
        private const val KEY_ACCEPTED = "medication_notice_accepted"
    }
}
