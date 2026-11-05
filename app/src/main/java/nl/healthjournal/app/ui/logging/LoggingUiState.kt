package nl.healthjournal.app.ui.logging

import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.NhgBloodPressureCategory
import nl.healthjournal.domain.metrics.model.NhgBmiCategory
import nl.healthjournal.domain.metrics.model.NhgGlucoseCategory
import nl.healthjournal.domain.metrics.model.NhgWaistCircumferenceCategory
import nl.healthjournal.domain.metrics.model.OxygenContext
import nl.healthjournal.domain.metrics.model.OxygenSite
import nl.healthjournal.domain.profile.model.Profile
import java.math.BigDecimal

enum class MetricType {
    WEIGHT,
    BLOOD_PRESSURE,
    GLUCOSE,
    ACTIVITY,
    WAIST_CIRCUMFERENCE,
    OXYGEN_SATURATION
}

/** The picker values the person has to set themselves when there is no earlier entry to start from. */
enum class LogField { WEIGHT, SYSTOLIC, DIASTOLIC, PULSE, WAIST, OXYGEN }

data class LoggingUiState(
    val activeProfile: Profile? = null,
    /** False until the first read of the profile has finished, so "no profile" is not shown while loading. */
    val profileLoaded: Boolean = false,
    val selectedMetric: MetricType = MetricType.WEIGHT,
    /** The metrics in the order the person chose, Migraine included. */
    val orderedMetrics: List<MetricSlot> = MetricSlot.DEFAULT_ORDER,

    // Weight inputs & feedback
    val weightValue: Double = START_WEIGHT_KG,
    val weightInput: String = START_WEIGHT_KG.toString(),
    val previewBmi: BigDecimal? = null,
    val previewBmiCategory: NhgBmiCategory? = null,

    // BP inputs & feedback
    val systolicValue: Int = START_SYSTOLIC,
    val systolicInput: String = START_SYSTOLIC.toString(),
    val diastolicValue: Int = START_DIASTOLIC,
    val diastolicInput: String = START_DIASTOLIC.toString(),
    val pulseValue: Int = START_PULSE,
    val previewBpCategory: NhgBloodPressureCategory? = null,

    // Glucose inputs & feedback
    val glucoseInput: String = "",
    val glucoseContext: GlucoseContext = GlucoseContext.FASTING,
    val previewGlucoseCategory: NhgGlucoseCategory? = null,

    // Activity inputs
    val activityDurationInput: String = "",
    val activityDistanceInput: String = "",

    // Waist inputs & feedback
    val waistValue: Double = START_WAIST_CM,
    val waistInput: String = START_WAIST_CM.toString(),
    val previewWaistCategory: NhgWaistCircumferenceCategory? = null,

    // Oxygen saturation inputs: a value that stays unset until the person picks it
    val oxygenPercentValue: Int = START_OXYGEN_PERCENT,
    val oxygenPulseInput: String = "",
    val oxygenContext: OxygenContext? = null,
    val oxygenSite: OxygenSite? = null,
    val oxygenGiven: Boolean = false,

    // Optional comment for the entry being logged (one draft, cleared after save and on tab change)
    val commentInput: String = "",

    /**
     * The picker values the person set or that come from their own latest entry. A picker without an earlier
     * entry only shows a starting position: it is not a value of theirs until they move it, and it is never
     * classified or recorded.
     */
    val chosen: Set<LogField> = emptySet(),

    val isSaving: Boolean = false,
    val successMessage: UiText? = null,
    val errorMessage: UiText? = null
) {
    /** Whether the selected tab holds a value the person set, so the entry can be recorded. */
    val canSave: Boolean
        get() = when (selectedMetric) {
            MetricType.WEIGHT -> LogField.WEIGHT in chosen
            MetricType.BLOOD_PRESSURE -> LogField.SYSTOLIC in chosen && LogField.DIASTOLIC in chosen
            MetricType.WAIST_CIRCUMFERENCE -> LogField.WAIST in chosen
            MetricType.OXYGEN_SATURATION -> LogField.OXYGEN in chosen
            MetricType.GLUCOSE, MetricType.ACTIVITY -> true
        }
}

// Where a picker starts when there is no earlier entry. These are scroll positions, not targets or advice.
private const val START_WEIGHT_KG = 75.0
private const val START_SYSTOLIC = 120
private const val START_DIASTOLIC = 80
private const val START_PULSE = 70
private const val START_WAIST_CM = 90.0
private const val START_OXYGEN_PERCENT = 96
