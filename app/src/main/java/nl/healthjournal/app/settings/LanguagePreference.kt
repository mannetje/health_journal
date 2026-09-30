package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** The text language of the app. [SYSTEM] follows the device. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    DUTCH("nl");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * The region that drives date, time and number formats, independent of the text language
 * (English text with Dutch formats is a valid combination). [SYSTEM] follows the device region.
 */
enum class AppRegion(val tag: String?) {
    SYSTEM(null),
    NETHERLANDS("NL"),
    UNITED_STATES("US");

    companion object {
        fun fromTag(tag: String?): AppRegion = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

class LanguagePreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var language: AppLanguage
        get() = AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.tag).apply()

    var region: AppRegion
        get() = AppRegion.fromTag(prefs.getString(KEY_REGION, null))
        set(value) = prefs.edit().putString(KEY_REGION, value.tag).apply()

    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "language_tag"
        private const val KEY_REGION = "region_tag"
    }
}

/**
 * Combines the chosen language and region with the device locale. Each part that is left on
 * "System" comes from the device, so a device set to English (Netherlands) keeps Dutch formats
 * when only the language is overridden. Returns null when both follow the system.
 */
fun resolveAppLocale(language: AppLanguage, region: AppRegion, system: Locale): Locale? {
    if (language == AppLanguage.SYSTEM && region == AppRegion.SYSTEM) return null
    return Locale.Builder()
        .setLanguage(language.tag ?: system.language)
        .setRegion(region.tag ?: system.country)
        .build()
}

/**
 * Returns a context whose resources use the chosen language and region, and sets the process
 * default locale so `java.time` and `String.format` use the same region formats.
 */
fun Context.withAppLocale(language: AppLanguage, region: AppRegion): Context {
    val system = Resources.getSystem().configuration.locales[0]
    val locale = resolveAppLocale(language, region, system)
    Locale.setDefault(locale ?: system)
    if (locale == null) return this
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
