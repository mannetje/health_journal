package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Opt-in setting to enable WindowManager.LayoutParams.FLAG_SECURE, hiding content
 * in the recent apps switcher and blocking screenshots.
 */
class SecureScreenPreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_SECURE_SCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_SECURE_SCREEN, value).apply()

    companion object {
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_SECURE_SCREEN = "secure_screen_enabled"
    }
}
