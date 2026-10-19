package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import nl.healthjournal.domain.model.lock.LockTimeout

/**
 * Stores the opt-in app lock configuration: whether the lock is enabled and the background timeout.
 */
class AppLockPreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var timeout: LockTimeout
        get() = LockTimeout.fromSeconds(prefs.getInt(KEY_TIMEOUT_SECONDS, LockTimeout.IMMEDIATELY.seconds))
        set(value) = prefs.edit().putInt(KEY_TIMEOUT_SECONDS, value.seconds).apply()

    companion object {
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_TIMEOUT_SECONDS = "app_lock_timeout_seconds"
    }
}
