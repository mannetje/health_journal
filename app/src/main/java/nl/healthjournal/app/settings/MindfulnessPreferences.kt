package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import nl.healthjournal.app.ui.mindfulness.BreathingVisual
import nl.healthjournal.domain.model.mindfulness.BreathingDuration
import nl.healthjournal.domain.model.mindfulness.BreathingPatternId
import nl.healthjournal.domain.model.mindfulness.CueChoices
import nl.healthjournal.domain.model.mindfulness.MeditationTimer

/**
 * The display choices of the Mindfulness area, so the setup screens open as the user left them. Only these
 * choices are stored: no sessions, times, counts or history. A value that is missing or no longer valid falls
 * back to the default.
 */
class MindfulnessPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var pattern: BreathingPatternId
        get() = enumOrDefault(prefs.getString(KEY_PATTERN, null), BreathingPatternId.BELLY)
        set(value) = prefs.edit().putString(KEY_PATTERN, value.name).apply()

    var duration: BreathingDuration
        get() = enumOrDefault(prefs.getString(KEY_DURATION, null), BreathingDuration.THREE_MINUTES)
        set(value) = prefs.edit().putString(KEY_DURATION, value.name).apply()

    var visual: BreathingVisual
        get() = enumOrDefault(prefs.getString(KEY_VISUAL, null), BreathingVisual.LOTUS)
        set(value) = prefs.edit().putString(KEY_VISUAL, value.name).apply()

    var meditateMinutes: Int
        get() = prefs.getInt(KEY_MEDITATE_MINUTES, DEFAULT_MEDITATE_MINUTES)
            .takeIf { it in MeditationTimer.PRESET_MINUTES } ?: DEFAULT_MEDITATE_MINUTES
        set(value) = prefs.edit().putInt(KEY_MEDITATE_MINUTES, value).apply()

    /** Minutes between interval bells; null for none. */
    var meditateInterval: Int?
        get() = prefs.getInt(KEY_MEDITATE_INTERVAL, 0).takeIf { it in MeditationTimer.INTERVAL_PRESET_MINUTES }
        set(value) = prefs.edit().putInt(KEY_MEDITATE_INTERVAL, value ?: 0).apply()

    /** Whether the first-open safety note has been acknowledged. */
    var safetyAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_SAFETY, false)
        set(value) = prefs.edit().putBoolean(KEY_SAFETY, value).apply()

    var breatheCues: CueChoices
        get() = readCues("breathe", CueChoices())
        set(value) = writeCues("breathe", value)

    var meditateCues: CueChoices
        get() = readCues("meditate", CueChoices(tones = true))
        set(value) = writeCues("meditate", value)

    private fun readCues(prefix: String, default: CueChoices) = CueChoices(
        tones = prefs.getBoolean("${prefix}_tones", default.tones),
        haptics = prefs.getBoolean("${prefix}_haptics", default.haptics),
        speech = prefs.getBoolean("${prefix}_speech", default.speech),
        voiceName = prefs.getString("${prefix}_voice", null),
    )

    private fun writeCues(prefix: String, cues: CueChoices) {
        prefs.edit()
            .putBoolean("${prefix}_tones", cues.tones)
            .putBoolean("${prefix}_haptics", cues.haptics)
            .putBoolean("${prefix}_speech", cues.speech)
            .putString("${prefix}_voice", cues.voiceName)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "mindfulness_prefs"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_DURATION = "duration"
        private const val KEY_VISUAL = "visual"
        private const val KEY_SAFETY = "safety_acknowledged"
        private const val KEY_MEDITATE_MINUTES = "meditate_minutes"
        private const val KEY_MEDITATE_INTERVAL = "meditate_interval"
        private val DEFAULT_MEDITATE_MINUTES = MeditationTimer.PRESET_MINUTES.first()
    }
}

/** The enum constant called [name], or [default] when [name] is null or unknown. */
internal inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
