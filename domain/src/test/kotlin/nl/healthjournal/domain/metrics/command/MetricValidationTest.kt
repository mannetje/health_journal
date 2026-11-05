package nl.healthjournal.domain.metrics.command

import kotlinx.coroutines.runBlocking
import nl.healthjournal.domain.metrics.model.ActivitySession
import nl.healthjournal.domain.metrics.model.BloodPressureReading
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.GlucoseLevel
import nl.healthjournal.domain.metrics.model.OxygenSaturationPercent
import nl.healthjournal.domain.metrics.model.WaistCircumferenceCm
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.profile.model.Sex
import nl.healthjournal.domain.shared.command.DomainError
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.ValidationField
import nl.healthjournal.domain.shared.command.ValidationField.COMMENT
import nl.healthjournal.domain.shared.command.ValidationField.DIASTOLIC
import nl.healthjournal.domain.shared.command.ValidationField.DISTANCE
import nl.healthjournal.domain.shared.command.ValidationField.GLUCOSE
import nl.healthjournal.domain.shared.command.ValidationField.OXYGEN_SATURATION
import nl.healthjournal.domain.shared.command.ValidationField.PULSE
import nl.healthjournal.domain.shared.command.ValidationField.SYSTOLIC
import nl.healthjournal.domain.shared.command.ValidationField.TIME_RANGE
import nl.healthjournal.domain.shared.command.ValidationField.WAIST
import nl.healthjournal.domain.shared.command.ValidationField.WEIGHT
import nl.healthjournal.domain.shared.command.ValidationRule
import nl.healthjournal.domain.shared.command.ValidationRule.END_NOT_AFTER_START
import nl.healthjournal.domain.shared.command.ValidationRule.OUT_OF_RANGE
import nl.healthjournal.domain.shared.command.ValidationRule.REQUIRED
import nl.healthjournal.domain.shared.command.ValidationRule.SYSTOLIC_NOT_ABOVE_DIASTOLIC
import nl.healthjournal.domain.shared.command.ValidationRule.TOO_LONG
import nl.healthjournal.domain.shared.model.EntryComment
import nl.healthjournal.domain.shared.model.HeightCm
import nl.healthjournal.domain.shared.model.MeasurementId
import nl.healthjournal.domain.shared.model.WeightKg
import nl.healthjournal.domain.testing.FakeHealthLogRepository
import nl.healthjournal.domain.testing.FakeProfileRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Input a user can plausibly type must come back as `DomainError.Invalid(field, rule, params)`: never as
 * `DomainError.Storage` (which the UI shows as "could not save"), and never as a thrown exception.
 *
 * Every case below runs through the Record handler and, where the command has the field, the Update handler,
 * and also checks that nothing was written. The bounds come from the value objects, so a changed bound changes
 * the expectation with it, and a case that stops being invalid is reported.
 */
class MetricValidationTest {
    private val at = Instant.parse("2026-01-02T03:04:05Z")
    private val clock = Clock.fixed(Instant.parse("2026-09-20T10:15:00Z"), ZoneOffset.UTC)
    private val health = FakeHealthLogRepository()
    private val profiles = FakeProfileRepository()
    private val pid = Profile.create(
        name = "Test", dateOfBirth = LocalDate.of(1980, 1, 1), height = HeightCm(180), sex = Sex.FEMALE
    ).also { profiles.profiles[it.id] = it }.id

    /** One input, offered to the Record handler and, when the Update command has the same field, to the Update handler. */
    private class Attempt(
        val record: suspend () -> DomainResult<*>,
        val update: (suspend () -> DomainResult<*>)? = null
    )

    private class Case(val label: String, val attempt: Attempt, val expected: DomainError.Invalid?)

    private fun weight(kg: String = "80", comment: String? = null) = Attempt(
        { RecordWeightHandler(health, profiles, clock).handle(RecordWeightCommand(pid, BigDecimal(kg), comment = comment)) },
        { UpdateWeightHandler(health, profiles).handle(UpdateWeightCommand(MeasurementId.generate(), pid, BigDecimal(kg), at, comment)) }
    )

    private fun bloodPressure(systolic: Int = 120, diastolic: Int = 80, pulse: Int? = null, comment: String? = null) = Attempt(
        { RecordBloodPressureHandler(health, clock).handle(RecordBloodPressureCommand(pid, systolic, diastolic, pulse, comment = comment)) },
        { UpdateBloodPressureHandler(health).handle(UpdateBloodPressureCommand(MeasurementId.generate(), pid, systolic, diastolic, at, pulse, comment)) }
    )

    private fun glucose(mmol: String? = "5.5", mgDl: String? = null, comment: String? = null) = Attempt(
        {
            RecordGlucoseHandler(health, clock).handle(
                RecordGlucoseCommand(pid, GlucoseContext.FASTING, mmol?.let(::BigDecimal), mgDl?.let(::BigDecimal), comment = comment)
            )
        },
        // The Update command only carries mmol/L, so a mg/dL-only or empty input has no Update form.
        mmol?.let { value ->
            suspend {
                UpdateGlucoseHandler(health).handle(
                    UpdateGlucoseCommand(MeasurementId.generate(), pid, GlucoseContext.FASTING, BigDecimal(value), at, comment)
                )
            }
        }
    )

    private fun oxygen(percent: Int = 97, pulse: Int? = null, comment: String? = null) = Attempt(
        { RecordOxygenSaturationHandler(health, clock).handle(RecordOxygenSaturationCommand(pid, percent, pulse, comment = comment)) },
        {
            UpdateOxygenSaturationHandler(health).handle(
                UpdateOxygenSaturationCommand(MeasurementId.generate(), pid, percent, pulse, null, null, false, at, comment)
            )
        }
    )

    private fun waist(cm: Double = 90.0, comment: String? = null) = Attempt(
        { RecordWaistCircumferenceHandler(health, profiles, clock).handle(RecordWaistCircumferenceCommand(pid, cm, comment = comment)) },
        { UpdateWaistCircumferenceHandler(health, profiles).handle(UpdateWaistCircumferenceCommand(MeasurementId.generate(), pid, cm, at, comment)) }
    )

    private fun activity(distance: Double = 1000.0, end: Instant = at.plusSeconds(600), comment: String? = null) = Attempt(
        { RecordActivityHandler(health).handle(RecordActivityCommand(pid, at, end, distance, comment = comment)) },
        { UpdateActivityHandler(health).handle(UpdateActivityCommand(MeasurementId.generate(), pid, at, end, distance, comment)) }
    )

    private fun invalid(label: String, attempt: Attempt, field: ValidationField, rule: ValidationRule, vararg params: Any) =
        Case(label, attempt, DomainError.Invalid(field, rule, params.toList()))

    private fun valid(label: String, attempt: Attempt) = Case(label, attempt, null)

    private val tooLong = "x".repeat(EntryComment.MAX_LENGTH + 1)
    private val longestAllowed = "x".repeat(EntryComment.MAX_LENGTH)
    private val commentRange = arrayOf<Any>(EntryComment.MAX_LENGTH)

    private val cases: List<Case> = listOf(
        // weight
        invalid("weight below minimum", weight("0.9"), WEIGHT, OUT_OF_RANGE, WeightKg.MIN_WEIGHT_KG, WeightKg.MAX_WEIGHT_KG),
        invalid("weight above maximum", weight("700.1"), WEIGHT, OUT_OF_RANGE, WeightKg.MIN_WEIGHT_KG, WeightKg.MAX_WEIGHT_KG),
        invalid("weight zero", weight("0"), WEIGHT, OUT_OF_RANGE, WeightKg.MIN_WEIGHT_KG, WeightKg.MAX_WEIGHT_KG),
        invalid("weight negative", weight("-80"), WEIGHT, OUT_OF_RANGE, WeightKg.MIN_WEIGHT_KG, WeightKg.MAX_WEIGHT_KG),
        invalid("weight comment too long", weight(comment = tooLong), COMMENT, TOO_LONG, *commentRange),
        valid("weight at minimum", weight("1.0")),
        valid("weight at maximum", weight("700.0")),

        // blood pressure
        invalid("systolic below minimum", bloodPressure(systolic = BloodPressureReading.MIN_SYSTOLIC - 1), SYSTOLIC, OUT_OF_RANGE, BloodPressureReading.MIN_SYSTOLIC, BloodPressureReading.MAX_SYSTOLIC),
        invalid("systolic above maximum", bloodPressure(systolic = BloodPressureReading.MAX_SYSTOLIC + 1), SYSTOLIC, OUT_OF_RANGE, BloodPressureReading.MIN_SYSTOLIC, BloodPressureReading.MAX_SYSTOLIC),
        invalid("diastolic below minimum", bloodPressure(diastolic = BloodPressureReading.MIN_DIASTOLIC - 1), DIASTOLIC, OUT_OF_RANGE, BloodPressureReading.MIN_DIASTOLIC, BloodPressureReading.MAX_DIASTOLIC),
        invalid("diastolic above maximum", bloodPressure(systolic = 300, diastolic = BloodPressureReading.MAX_DIASTOLIC + 1), DIASTOLIC, OUT_OF_RANGE, BloodPressureReading.MIN_DIASTOLIC, BloodPressureReading.MAX_DIASTOLIC),
        invalid("systolic equal to diastolic", bloodPressure(systolic = 100, diastolic = 100), SYSTOLIC, SYSTOLIC_NOT_ABOVE_DIASTOLIC),
        invalid("systolic below diastolic", bloodPressure(systolic = 80, diastolic = 120), SYSTOLIC, SYSTOLIC_NOT_ABOVE_DIASTOLIC),
        invalid("blood pressure pulse below minimum", bloodPressure(pulse = BloodPressureReading.MIN_PULSE - 1), PULSE, OUT_OF_RANGE, BloodPressureReading.MIN_PULSE, BloodPressureReading.MAX_PULSE),
        invalid("blood pressure pulse above maximum", bloodPressure(pulse = BloodPressureReading.MAX_PULSE + 1), PULSE, OUT_OF_RANGE, BloodPressureReading.MIN_PULSE, BloodPressureReading.MAX_PULSE),
        invalid("blood pressure comment too long", bloodPressure(comment = tooLong), COMMENT, TOO_LONG, *commentRange),
        valid("blood pressure at the lowest bounds", bloodPressure(BloodPressureReading.MIN_SYSTOLIC, BloodPressureReading.MIN_DIASTOLIC, BloodPressureReading.MIN_PULSE)),
        valid("blood pressure at the highest bounds", bloodPressure(BloodPressureReading.MAX_SYSTOLIC, BloodPressureReading.MAX_DIASTOLIC, BloodPressureReading.MAX_PULSE)),

        // glucose
        invalid("glucose below minimum", glucose("0.4"), GLUCOSE, OUT_OF_RANGE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE),
        invalid("glucose above maximum", glucose("55.1"), GLUCOSE, OUT_OF_RANGE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE),
        invalid("glucose negative", glucose("-1"), GLUCOSE, OUT_OF_RANGE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE),
        invalid("glucose in mg/dL below minimum", glucose(mmol = null, mgDl = "8"), GLUCOSE, OUT_OF_RANGE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE),
        invalid("glucose in mg/dL above maximum", glucose(mmol = null, mgDl = "1000"), GLUCOSE, OUT_OF_RANGE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE),
        invalid("glucose without a value", glucose(mmol = null, mgDl = null), GLUCOSE, REQUIRED),
        invalid("glucose comment too long", glucose(comment = tooLong), COMMENT, TOO_LONG, *commentRange),
        valid("glucose at minimum", glucose("0.5")),
        valid("glucose at maximum", glucose("55.0")),
        valid("glucose in mg/dL", glucose(mmol = null, mgDl = "100")),

        // oxygen saturation
        invalid("oxygen below minimum", oxygen(OxygenSaturationPercent.MIN - 1), OXYGEN_SATURATION, OUT_OF_RANGE, OxygenSaturationPercent.MIN, OxygenSaturationPercent.MAX),
        invalid("oxygen above maximum", oxygen(OxygenSaturationPercent.MAX + 1), OXYGEN_SATURATION, OUT_OF_RANGE, OxygenSaturationPercent.MIN, OxygenSaturationPercent.MAX),
        invalid("oxygen pulse below minimum", oxygen(pulse = BloodPressureReading.MIN_PULSE - 1), PULSE, OUT_OF_RANGE, BloodPressureReading.MIN_PULSE, BloodPressureReading.MAX_PULSE),
        invalid("oxygen pulse above maximum", oxygen(pulse = BloodPressureReading.MAX_PULSE + 1), PULSE, OUT_OF_RANGE, BloodPressureReading.MIN_PULSE, BloodPressureReading.MAX_PULSE),
        invalid("oxygen comment too long", oxygen(comment = tooLong), COMMENT, TOO_LONG, *commentRange),
        valid("oxygen at minimum", oxygen(OxygenSaturationPercent.MIN, BloodPressureReading.MIN_PULSE)),
        valid("oxygen at maximum", oxygen(OxygenSaturationPercent.MAX, BloodPressureReading.MAX_PULSE)),

        // waist circumference
        invalid("waist below minimum", waist(WaistCircumferenceCm.MIN_CM - 0.1), WAIST, OUT_OF_RANGE, WaistCircumferenceCm.MIN_CM, WaistCircumferenceCm.MAX_CM),
        invalid("waist above maximum", waist(WaistCircumferenceCm.MAX_CM + 0.1), WAIST, OUT_OF_RANGE, WaistCircumferenceCm.MIN_CM, WaistCircumferenceCm.MAX_CM),
        invalid("waist not a number", waist(Double.NaN), WAIST, OUT_OF_RANGE, WaistCircumferenceCm.MIN_CM, WaistCircumferenceCm.MAX_CM),
        invalid("waist infinite", waist(Double.POSITIVE_INFINITY), WAIST, OUT_OF_RANGE, WaistCircumferenceCm.MIN_CM, WaistCircumferenceCm.MAX_CM),
        invalid("waist comment too long", waist(comment = tooLong), COMMENT, TOO_LONG, *commentRange),
        valid("waist at minimum", waist(WaistCircumferenceCm.MIN_CM)),
        valid("waist at maximum", waist(WaistCircumferenceCm.MAX_CM)),

        // activity
        invalid("activity distance negative", activity(distance = -0.1), DISTANCE, OUT_OF_RANGE, 0.0, ActivitySession.MAX_DISTANCE_METERS),
        invalid("activity distance above maximum", activity(distance = ActivitySession.MAX_DISTANCE_METERS + 0.1), DISTANCE, OUT_OF_RANGE, 0.0, ActivitySession.MAX_DISTANCE_METERS),
        invalid("activity distance not a number", activity(distance = Double.NaN), DISTANCE, OUT_OF_RANGE, 0.0, ActivitySession.MAX_DISTANCE_METERS),
        invalid("activity ends when it starts", activity(end = at), TIME_RANGE, END_NOT_AFTER_START),
        invalid("activity ends before it starts", activity(end = at.minusSeconds(60)), TIME_RANGE, END_NOT_AFTER_START),
        invalid("activity comment too long", activity(comment = tooLong), COMMENT, TOO_LONG, *commentRange),
        valid("activity with no distance", activity(distance = 0.0)),
        valid("activity at maximum distance", activity(distance = ActivitySession.MAX_DISTANCE_METERS)),

        // comments that are fine on every metric
        valid("weight comment of the longest allowed length", weight(comment = longestAllowed)),
        valid("weight comment that is blank", weight(comment = "   ")),
        valid("weight comment with line breaks", weight(comment = "a\r\nb")),
        valid("blood pressure comment of the longest allowed length", bloodPressure(comment = longestAllowed)),
        valid("glucose comment of the longest allowed length", glucose(comment = longestAllowed)),
        valid("oxygen comment of the longest allowed length", oxygen(comment = longestAllowed)),
        valid("waist comment of the longest allowed length", waist(comment = longestAllowed)),
        valid("activity comment of the longest allowed length", activity(comment = longestAllowed))
    )

    private fun stored() = with(health) {
        weights.size + bloodPressures.size + glucoses.size + activities.size + waistCircumferences.size + oxygenSaturations.size
    }

    /** Runs [call] and describes how it deviates from [expected] (null means it must succeed or be a NotFound), or null when it does not. */
    private fun deviation(call: suspend () -> DomainResult<*>, expected: DomainError.Invalid?): String? {
        val result = try {
            runBlocking { call() }
        } catch (e: Throwable) {
            return "threw $e"
        }
        val error = result.errorOrNull()
        return when {
            expected == null && error == null -> null
            expected == null -> "expected success, got $error"
            error == expected -> null
            else -> "expected $expected, got ${error ?: "success"}"
        }
    }

    @Test fun `invalid input is a typed Invalid error from Record and Update, and nothing is saved`() {
        val problems = mutableListOf<String>()
        for (case in cases.filter { it.expected != null }) {
            val forms = listOfNotNull("record" to case.attempt.record, case.attempt.update?.let { "update" to it })
            for ((form, call) in forms) {
                val before = stored()
                deviation(call, case.expected)?.let { problems += "${case.label} ($form): $it" }
                if (stored() != before) problems += "${case.label} ($form): something was saved"
            }
        }
        assertEquals("cases that did not produce the expected Invalid error", emptyList<String>(), problems)
    }

    @Test fun `input at the bounds is accepted by Record`() {
        val problems = mutableListOf<String>()
        for (case in cases.filter { it.expected == null }) {
            deviation(case.attempt.record, null)?.let { problems += "${case.label}: $it" }
        }
        assertEquals("cases that should have been accepted", emptyList<String>(), problems)
    }

    @Test fun `an Update that passes validation reaches the repository and is reported as not found, not as invalid`() {
        // Every Update case above stops at validation. This pins the other side: valid input is not rejected.
        val problems = mutableListOf<String>()
        for (case in cases.filter { it.expected == null }) {
            val update = case.attempt.update ?: continue
            val error = try { runBlocking { update() }.errorOrNull() } catch (e: Throwable) { problems += "${case.label}: threw $e"; continue }
            if (error !is DomainError.NotFound) problems += "${case.label}: expected NotFound for an id that was never saved, got $error"
        }
        assertEquals(emptyList<String>(), problems)
    }

    @Test fun `every metric has a case for each of its bounds`() {
        // A guard on the table itself: a metric added without cases would otherwise pass silently.
        val labels = cases.map { it.label }
        for (metric in listOf("weight", "systolic", "diastolic", "glucose", "oxygen", "waist", "activity")) {
            check(labels.count { it.startsWith(metric) && cases.first { c -> c.label == it }.expected != null } >= 2) {
                "$metric has fewer than two invalid cases"
            }
        }
    }
}
