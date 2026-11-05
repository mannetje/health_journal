package nl.healthjournal.domain.model

import nl.healthjournal.domain.command.DomainError
import nl.healthjournal.domain.command.DomainResult
import nl.healthjournal.domain.command.ValidationField
import nl.healthjournal.domain.command.ValidationRule
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.common.EntryComment
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import nl.healthjournal.domain.model.metrics.OxygenSaturationEntry
import nl.healthjournal.domain.model.metrics.OxygenSaturationPercent
import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.common.WeightKg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ValueObjectFactoriesTest {

    private fun DomainResult<*>.invalid(): DomainError.Invalid =
        (errorOrNull() as? DomainError.Invalid) ?: error("expected Invalid, got $this")

    private fun DomainResult<*>.assertOutOfRange(field: ValidationField, min: Any, max: Any) {
        val e = invalid()
        assertEquals(field, e.field)
        assertEquals(ValidationRule.OUT_OF_RANGE, e.rule)
        assertEquals(listOf(min, max), e.params)
    }

    // --- blood pressure ---

    @Test
    fun `blood pressure accepts the bounds`() {
        assertTrue(BloodPressureReading.of(BloodPressureReading.MAX_SYSTOLIC, BloodPressureReading.MIN_DIASTOLIC).isSuccess)
        assertTrue(BloodPressureReading.of(BloodPressureReading.MIN_SYSTOLIC, BloodPressureReading.MIN_DIASTOLIC).isSuccess)
        assertTrue(BloodPressureReading.of(120, 80, BloodPressureReading.MAX_PULSE).isSuccess)
        assertTrue(BloodPressureReading.of(120, 80, BloodPressureReading.MIN_PULSE).isSuccess)
    }

    @Test
    fun `blood pressure rejects systolic outside its bounds`() {
        BloodPressureReading.of(BloodPressureReading.MIN_SYSTOLIC - 1, 30)
            .assertOutOfRange(ValidationField.SYSTOLIC, BloodPressureReading.MIN_SYSTOLIC, BloodPressureReading.MAX_SYSTOLIC)
        BloodPressureReading.of(BloodPressureReading.MAX_SYSTOLIC + 1, 80)
            .assertOutOfRange(ValidationField.SYSTOLIC, BloodPressureReading.MIN_SYSTOLIC, BloodPressureReading.MAX_SYSTOLIC)
    }

    @Test
    fun `blood pressure rejects diastolic outside its bounds`() {
        BloodPressureReading.of(120, BloodPressureReading.MIN_DIASTOLIC - 1)
            .assertOutOfRange(ValidationField.DIASTOLIC, BloodPressureReading.MIN_DIASTOLIC, BloodPressureReading.MAX_DIASTOLIC)
        BloodPressureReading.of(300, BloodPressureReading.MAX_DIASTOLIC + 1)
            .assertOutOfRange(ValidationField.DIASTOLIC, BloodPressureReading.MIN_DIASTOLIC, BloodPressureReading.MAX_DIASTOLIC)
    }

    @Test
    fun `blood pressure rejects diastolic not below systolic`() {
        assertEquals(ValidationRule.SYSTOLIC_NOT_ABOVE_DIASTOLIC, BloodPressureReading.of(100, 100).invalid().rule)
        assertEquals(ValidationRule.SYSTOLIC_NOT_ABOVE_DIASTOLIC, BloodPressureReading.of(90, 100).invalid().rule)
        assertTrue(BloodPressureReading.of(101, 100).isSuccess)
    }

    @Test
    fun `blood pressure rejects pulse outside its bounds`() {
        BloodPressureReading.of(120, 80, BloodPressureReading.MIN_PULSE - 1)
            .assertOutOfRange(ValidationField.PULSE, BloodPressureReading.MIN_PULSE, BloodPressureReading.MAX_PULSE)
        BloodPressureReading.of(120, 80, BloodPressureReading.MAX_PULSE + 1)
            .assertOutOfRange(ValidationField.PULSE, BloodPressureReading.MIN_PULSE, BloodPressureReading.MAX_PULSE)
    }

    @Test
    fun `blood pressure factory and constructor agree`() {
        for ((s, d) in listOf(120 to 80, 39 to 20, 301 to 80, 100 to 100, 120 to 19)) {
            val ok = BloodPressureReading.of(s, d).isSuccess
            val constructed = runCatching { BloodPressureReading(s, d) }.isSuccess
            assertEquals("$s/$d", constructed, ok)
        }
    }

    // --- glucose ---

    @Test
    fun `glucose accepts the bounds and rejects outside them`() {
        assertTrue(GlucoseLevel.of(GlucoseLevel.MIN_GLUCOSE).isSuccess)
        assertTrue(GlucoseLevel.of(GlucoseLevel.MAX_GLUCOSE).isSuccess)
        GlucoseLevel.of(BigDecimal("0.4"))
            .assertOutOfRange(ValidationField.GLUCOSE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE)
        GlucoseLevel.of(BigDecimal("55.1"))
            .assertOutOfRange(ValidationField.GLUCOSE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE)
    }

    @Test
    fun `glucose in mg per dl is checked after conversion`() {
        assertEquals(BigDecimal("5.55"), GlucoseLevel.ofMgDl(BigDecimal("100")).getOrNull()?.valueInMmolL)
        GlucoseLevel.ofMgDl(BigDecimal("1"))
            .assertOutOfRange(ValidationField.GLUCOSE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE)
        GlucoseLevel.ofMgDl(BigDecimal("2000"))
            .assertOutOfRange(ValidationField.GLUCOSE, GlucoseLevel.MIN_GLUCOSE, GlucoseLevel.MAX_GLUCOSE)
    }

    // --- weight ---

    @Test
    fun `weight accepts the bounds and rejects outside them`() {
        assertTrue(WeightKg.of(WeightKg.MIN_WEIGHT_KG).isSuccess)
        assertTrue(WeightKg.of(WeightKg.MAX_WEIGHT_KG).isSuccess)
        WeightKg.of(BigDecimal("0.9")).assertOutOfRange(ValidationField.WEIGHT, WeightKg.MIN_WEIGHT_KG, WeightKg.MAX_WEIGHT_KG)
        WeightKg.of(BigDecimal("700.1")).assertOutOfRange(ValidationField.WEIGHT, WeightKg.MIN_WEIGHT_KG, WeightKg.MAX_WEIGHT_KG)
    }

    // --- waist ---

    @Test
    fun `waist accepts the bounds and rejects outside them`() {
        assertTrue(WaistCircumferenceCm.of(WaistCircumferenceCm.MIN_CM).isSuccess)
        assertTrue(WaistCircumferenceCm.of(WaistCircumferenceCm.MAX_CM).isSuccess)
        WaistCircumferenceCm.of(39.9)
            .assertOutOfRange(ValidationField.WAIST, WaistCircumferenceCm.MIN_CM, WaistCircumferenceCm.MAX_CM)
        WaistCircumferenceCm.of(200.1)
            .assertOutOfRange(ValidationField.WAIST, WaistCircumferenceCm.MIN_CM, WaistCircumferenceCm.MAX_CM)
    }

    // --- oxygen saturation ---

    @Test
    fun `oxygen saturation accepts the bounds and rejects outside them`() {
        assertTrue(OxygenSaturationPercent.of(OxygenSaturationPercent.MIN).isSuccess)
        assertTrue(OxygenSaturationPercent.of(OxygenSaturationPercent.MAX).isSuccess)
        OxygenSaturationPercent.of(49)
            .assertOutOfRange(ValidationField.OXYGEN_SATURATION, OxygenSaturationPercent.MIN, OxygenSaturationPercent.MAX)
        OxygenSaturationPercent.of(101)
            .assertOutOfRange(ValidationField.OXYGEN_SATURATION, OxygenSaturationPercent.MIN, OxygenSaturationPercent.MAX)
    }

    @Test
    fun `oxygen saturation pulse follows the heart rate bounds`() {
        assertNull(OxygenSaturationEntry.pulseError(OxygenSaturationEntry.MIN_PULSE))
        assertNull(OxygenSaturationEntry.pulseError(OxygenSaturationEntry.MAX_PULSE))
        assertEquals(ValidationField.PULSE, OxygenSaturationEntry.pulseError(OxygenSaturationEntry.MIN_PULSE - 1)?.field)
        assertEquals(ValidationField.PULSE, OxygenSaturationEntry.pulseError(OxygenSaturationEntry.MAX_PULSE + 1)?.field)
    }

    // --- activity ---

    private val start = Instant.parse("2026-09-20T08:00:00Z")

    private fun session(end: Instant, distance: Double) =
        ActivitySession.of(MeasurementId.generate(), ProfileId.generate(), start, end, distance)

    @Test
    fun `activity needs the end after the start`() {
        assertTrue(session(start.plusSeconds(1), 0.0).isSuccess)
        assertEquals(ValidationRule.END_NOT_AFTER_START, session(start, 10.0).invalid().rule)
        assertEquals(ValidationField.TIME_RANGE, session(start.minusSeconds(1), 10.0).invalid().field)
    }

    @Test
    fun `activity distance is bounded`() {
        assertTrue(session(start.plusSeconds(60), ActivitySession.MAX_DISTANCE_METERS).isSuccess)
        session(start.plusSeconds(60), -0.1)
            .assertOutOfRange(ValidationField.DISTANCE, 0.0, ActivitySession.MAX_DISTANCE_METERS)
        session(start.plusSeconds(60), ActivitySession.MAX_DISTANCE_METERS + 1)
            .assertOutOfRange(ValidationField.DISTANCE, 0.0, ActivitySession.MAX_DISTANCE_METERS)
    }

    // --- comment ---

    @Test
    fun `comment factory returns null for blank and an error when too long`() {
        assertNull(EntryComment.of("  ").getOrNull())
        assertEquals("a b", EntryComment.of(" a\nb ").getOrNull()?.text)
        val tooLong = EntryComment.of("x".repeat(EntryComment.MAX_LENGTH + 1)).invalid()
        assertEquals(ValidationRule.TOO_LONG, tooLong.rule)
        assertEquals(listOf(EntryComment.MAX_LENGTH), tooLong.params)
    }

    @Test
    fun `constructors still throw`() {
        assertThrows(IllegalArgumentException::class.java) { BloodPressureReading(100, 100) }
        assertThrows(IllegalArgumentException::class.java) { WaistCircumferenceCm(10.0) }
    }
}
