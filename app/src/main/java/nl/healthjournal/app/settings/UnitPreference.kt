package nl.healthjournal.app.settings

import android.content.Context
import android.content.SharedPreferences
import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import nl.healthjournal.domain.model.common.GlucoseUnit
import nl.healthjournal.domain.model.common.UnitSystem
import java.util.Locale

/** The measurement system the user picked. [SYSTEM] follows the region of the active locale. */
enum class UnitSystemChoice(val tag: String?) {
    SYSTEM(null),
    METRIC("metric"),
    IMPERIAL("imperial");

    companion object {
        fun fromTag(tag: String?): UnitSystemChoice = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/** The glucose unit the user picked. [SYSTEM] follows the region of the active locale. */
enum class GlucoseUnitChoice(val tag: String?) {
    SYSTEM(null),
    MMOL("mmol"),
    MGDL("mgdl");

    companion object {
        fun fromTag(tag: String?): GlucoseUnitChoice = entries.firstOrNull { it.tag == tag } ?: SYSTEM

        fun from(unit: GlucoseUnit): GlucoseUnitChoice =
            if (unit == GlucoseUnit.MG_PER_DL) MGDL else MMOL
    }
}

/** The units the UI shows and accepts. Storage is metric regardless (ADR 0014). */
data class DisplayUnits(val system: UnitSystem, val glucose: GlucoseUnit) {
    val isImperial: Boolean get() = system == UnitSystem.IMPERIAL
    val weightSymbol: String get() = if (isImperial) "lb" else "kg"
    val distanceSymbol: String get() = if (isImperial) "mi" else "km"
    val glucoseSymbol: String get() = if (glucose == GlucoseUnit.MG_PER_DL) "mg/dL" else "mmol/L"

    companion object {
        val DEFAULT = DisplayUnits(UnitSystem.METRIC, GlucoseUnit.MMOL_PER_L)
    }
}

class UnitPreference(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var system: UnitSystemChoice
        get() = UnitSystemChoice.fromTag(prefs.getString(KEY_SYSTEM, null))
        set(value) = prefs.edit().putString(KEY_SYSTEM, value.tag).apply()

    var glucose: GlucoseUnitChoice
        get() = GlucoseUnitChoice.fromTag(prefs.getString(KEY_GLUCOSE, null))
        set(value) = prefs.edit().putString(KEY_GLUCOSE, value.tag).apply()

    /** Resolves the saved choices against the active locale (the app's language and region). */
    fun resolve(locale: Locale = Locale.getDefault()): DisplayUnits =
        resolveDisplayUnits(system, glucose, locale)

    companion object {
        private const val PREFS_NAME = "unit_prefs"
        private const val KEY_SYSTEM = "unit_system"
        private const val KEY_GLUCOSE = "glucose_unit"
    }
}

// Regions where blood glucose is normally reported in mg/dL. Everywhere else uses mmol/L.
private val MG_DL_REGIONS = setOf(
    "US", "JP", "DE", "FR", "ES", "IT", "AT", "BE", "PT", "GR", "IN", "BR", "AR", "CL", "CO", "MX",
    "EG", "IL", "SA", "KR", "TW", "TR"
)

// Fallback for API 26-27, where ICU measurement systems are not exposed. Imperial body and distance units.
private val IMPERIAL_REGIONS = setOf("US", "LR", "MM", "GB")

/**
 * Follows the locale's measurement system as Android's ICU data defines it (US and UK use
 * imperial units, the rest of the world metric). Called with the app's active locale, so the
 * Regional formats setting decides the default and a device set to English (Netherlands) is metric.
 */
fun resolveDisplayUnits(system: UnitSystemChoice, glucose: GlucoseUnitChoice, locale: Locale): DisplayUnits {
    val resolvedSystem = when (system) {
        UnitSystemChoice.METRIC -> UnitSystem.METRIC
        UnitSystemChoice.IMPERIAL -> UnitSystem.IMPERIAL
        UnitSystemChoice.SYSTEM -> if (localeUsesImperial(locale)) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }
    val resolvedGlucose = when (glucose) {
        GlucoseUnitChoice.MMOL -> GlucoseUnit.MMOL_PER_L
        GlucoseUnitChoice.MGDL -> GlucoseUnit.MG_PER_DL
        GlucoseUnitChoice.SYSTEM ->
            if (locale.country in MG_DL_REGIONS) GlucoseUnit.MG_PER_DL else GlucoseUnit.MMOL_PER_L
    }
    return DisplayUnits(resolvedSystem, resolvedGlucose)
}

private fun localeUsesImperial(locale: Locale): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        LocaleData.getMeasurementSystem(ULocale.forLocale(locale)) != LocaleData.MeasurementSystem.SI
    } else {
        locale.country in IMPERIAL_REGIONS
    }
