package nl.healthjournal.app.ui.logging

import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.parseDecimal
import nl.healthjournal.app.ui.common.weightToKg
import nl.healthjournal.domain.metrics.model.BloodPressureReading
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.GlucoseLevel
import nl.healthjournal.domain.metrics.model.NhgBloodPressureCategory
import nl.healthjournal.domain.metrics.model.NhgBmiCategory
import nl.healthjournal.domain.metrics.model.NhgGlucoseCategory
import nl.healthjournal.domain.metrics.model.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.metrics.model.WaistCircumferenceCm
import nl.healthjournal.domain.metrics.model.WaistCircumferenceClassifier
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.shared.model.GlucoseUnit
import nl.healthjournal.domain.shared.model.UnitConversion
import nl.healthjournal.domain.shared.model.WeightKg
import java.math.BigDecimal

/**
 * The live feedback shown while the person types. Each function returns null when there is nothing to show:
 * the text is not a number, it is outside the physical range, or the person has not set the value yet
 * (a picker's starting position is never classified).
 */
internal class BmiPreview(val bmi: BigDecimal, val category: NhgBmiCategory)

internal fun weightPreview(input: String, units: DisplayUnits, profile: Profile?, chosen: Set<LogField>): BmiPreview? {
    val weightKg = input.parseDecimal()?.let { units.weightToKg(it) }
    if (weightKg == null || profile == null || LogField.WEIGHT !in chosen) return null
    if (weightKg !in WeightKg.MIN_WEIGHT_KG.toDouble()..WeightKg.MAX_WEIGHT_KG.toDouble()) return null
    val bmi = profile.calculateBmi(WeightKg(UnitConversion.toStoredKg(weightKg)))?.bmi ?: return null
    return BmiPreview(bmi, NhgBmiCategory.classify(bmi))
}

internal fun bloodPressurePreview(systolic: String, diastolic: String, pulse: Int?, chosen: Set<LogField>): NhgBloodPressureCategory? {
    val sys = systolic.toIntOrNull()
    val dia = diastolic.toIntOrNull()
    if (sys == null || dia == null) return null
    if (LogField.SYSTOLIC !in chosen || LogField.DIASTOLIC !in chosen) return null
    if (sys !in BloodPressureReading.MIN_SYSTOLIC..BloodPressureReading.MAX_SYSTOLIC) return null
    if (dia !in BloodPressureReading.MIN_DIASTOLIC..BloodPressureReading.MAX_DIASTOLIC) return null
    if (sys <= dia) return null
    return NhgBloodPressureCategory.classify(BloodPressureReading(systolic = sys, diastolic = dia, pulse = pulse))
}

internal fun waistPreview(input: String, profile: Profile?, chosen: Set<LogField>): NhgWaistCircumferenceCategory? {
    val cm = input.toDoubleOrNull()
    if (cm == null || profile == null || LogField.WAIST !in chosen) return null
    if (cm !in WaistCircumferenceCm.MIN_CM..WaistCircumferenceCm.MAX_CM) return null
    return WaistCircumferenceClassifier.classify(profile.sex, WaistCircumferenceCm(cm))
}

internal fun glucosePreview(input: String, context: GlucoseContext, units: DisplayUnits): NhgGlucoseCategory? {
    val value = input.parseDecimal()?.takeIf { it > 0 } ?: return null
    // A value outside the physical range has no category; the preview just stays empty
    val level = runCatching {
        if (units.glucose == GlucoseUnit.MG_PER_DL) GlucoseLevel.fromMgDl(value) else GlucoseLevel(value)
    }.getOrNull() ?: return null
    return NhgGlucoseCategory.classify(level, context)
}
