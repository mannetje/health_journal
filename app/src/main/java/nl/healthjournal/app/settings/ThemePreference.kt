package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences

/** The theme the user picked. [SYSTEM] follows the device's light or dark setting. */
enum class ThemeChoice(val tag: String?) {
    SYSTEM(null),
    LIGHT("light"),
    DARK("dark");

    /** Whether the dark palette applies, given what the system currently uses. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromTag(tag: String?): ThemeChoice = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

class ThemePreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var choice: ThemeChoice
        get() = ThemeChoice.fromTag(prefs.getString(KEY_THEME, null))
        set(value) = prefs.edit().putString(KEY_THEME, value.tag).apply()

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME = "theme_choice"
    }
}
