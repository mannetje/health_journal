package nl.healthjournal.app.ui.logging

import java.math.BigDecimal
import java.time.Instant
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.DisplayUnits
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.domain.metrics.model.GlucoseContext
import nl.healthjournal.domain.metrics.model.NhgBloodPressureCategory
import nl.healthjournal.domain.shared.model.GlucoseUnit
import nl.healthjournal.domain.shared.model.ProfileId
import nl.healthjournal.domain.shared.model.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Log form's text-to-command and live-preview rules, without a ViewModel, dispatcher or coroutine. */
class LogFormParsingTest {
    private val id = ProfileId.generate()
    private val metric = DisplayUnits.DEFAULT
    private val imperial = DisplayUnits(UnitSystem.IMPERIAL, GlucoseUnit.MG_PER_DL)

    private fun <C> FormResult<C>.ready(): C = (this as FormResult.Ready).command
    private fun FormResult<*>.message(): UiText = (this as FormResult.Unreadable).message

    @Test fun `weight accepts a decimal comma and stores kilograms`() {
        val state = LoggingUiState(weightInput = "80,5")
        assertEquals(80.5, weightCommand(id, state, metric).ready().weightKg.toDouble(), 0.0001)
    }

    @Test fun `weight in pounds is converted before the command is built`() {
        val state = LoggingUiState(weightInput = "100")
        assertEquals(45.36, weightCommand(id, state, imperial).ready().weightKg.toDouble(), 0.01)
    }

    @Test fun `unreadable weight names the unit shown`() {
        val state = LoggingUiState(weightInput = "abc")
        assertEquals(UiText.Res(R.string.log_err_weight, "lb"), weightCommand(id, state, imperial).message())
    }

    @Test fun `blood pressure reports the first unreadable value`() {
        assertEquals(UiText.Res(R.string.log_err_systolic), bloodPressureCommand(id, LoggingUiState(systolicInput = "x", diastolicInput = "80")).message())
        assertEquals(UiText.Res(R.string.log_err_diastolic), bloodPressureCommand(id, LoggingUiState(systolicInput = "120", diastolicInput = "")).message())
    }

    @Test fun `pulse is only sent when the person set it`() {
        val untouched = LoggingUiState(systolicInput = "120", diastolicInput = "80", pulseValue = 70)
        assertNull(bloodPressureCommand(id, untouched).ready().pulse)
        val set = untouched.copy(chosen = setOf(LogField.PULSE))
        assertEquals(70, bloodPressureCommand(id, set).ready().pulse)
    }

    @Test fun `glucose goes into the field of the unit shown`() {
        val state = LoggingUiState(glucoseInput = "5,5", glucoseContext = GlucoseContext.FASTING)
        val inMmol = glucoseCommand(id, state, metric).ready()
        assertEquals(BigDecimal.valueOf(5.5), inMmol.valueInMmolL)
        assertNull(inMmol.valueInMgDl)
        val inMg = glucoseCommand(id, state.copy(glucoseInput = "99"), imperial).ready()
        assertEquals(BigDecimal.valueOf(99.0), inMg.valueInMgDl)
        assertNull(inMg.valueInMmolL)
    }

    @Test fun `empty glucose is unreadable`() {
        assertEquals(UiText.Res(R.string.log_err_glucose), glucoseCommand(id, LoggingUiState(), metric).message())
    }

    @Test fun `activity ends now and starts the duration earlier`() {
        val now = Instant.parse("2026-01-01T12:00:00Z")
        val state = LoggingUiState(activityDurationInput = "30", activityDistanceInput = "5")
        val command = activityCommand(id, state, metric, now).ready()
        assertEquals(now, command.endTime)
        assertEquals(now.minusSeconds(1800), command.startTime)
        assertEquals(5000.0, command.distanceInMeters, 0.001)
    }

    @Test fun `activity rejects a zero duration and a negative distance`() {
        val now = Instant.EPOCH
        assertEquals(UiText.Res(R.string.log_err_duration), activityCommand(id, LoggingUiState(activityDurationInput = "0", activityDistanceInput = "1"), metric, now).message())
        assertEquals(UiText.Res(R.string.log_err_distance, "km"), activityCommand(id, LoggingUiState(activityDurationInput = "10", activityDistanceInput = "-1"), metric, now).message())
    }

    @Test fun `waist needs a number`() {
        assertEquals(UiText.Res(R.string.log_err_waist), waistCommand(id, LoggingUiState(waistInput = "")).message())
        assertEquals(88.5, waistCommand(id, LoggingUiState(waistInput = "88.5")).ready().waistCm, 0.0001)
    }

    @Test fun `oxygen pulse is optional and passes the choices through`() {
        val state = LoggingUiState(oxygenPercentValue = 94, oxygenPulseInput = "", oxygenGiven = true)
        val command = oxygenCommand(id, state)
        assertEquals(94, command.percent)
        assertNull(command.pulse)
        assertTrue(command.oxygenGiven)
        assertEquals(72, oxygenCommand(id, state.copy(oxygenPulseInput = "72")).pulse)
    }

    // --- previews ---

    private val bothChosen = setOf(LogField.SYSTOLIC, LogField.DIASTOLIC)

    @Test fun `blood pressure preview needs both values chosen and systolic above diastolic`() {
        assertEquals(NhgBloodPressureCategory.NORMAL, bloodPressurePreview("120", "80", null, bothChosen))
        assertEquals(NhgBloodPressureCategory.SERIOUSLY_RAISED, bloodPressurePreview("185", "95", null, bothChosen))
        assertNull(bloodPressurePreview("120", "80", null, setOf(LogField.SYSTOLIC)))
        assertNull(bloodPressurePreview("80", "120", null, bothChosen))
        assertNull(bloodPressurePreview("20", "10", null, bothChosen))
        assertNull(bloodPressurePreview("abc", "80", null, bothChosen))
    }

    @Test fun `weight and waist previews stay empty without a profile or a chosen value`() {
        assertNull(weightPreview("80", metric, null, setOf(LogField.WEIGHT)))
        assertNull(waistPreview("90", null, setOf(LogField.WAIST)))
    }

    @Test fun `glucose preview follows the unit and ignores impossible values`() {
        assertTrue(glucosePreview("5.5", GlucoseContext.FASTING, metric) != null)
        assertTrue(glucosePreview("99", GlucoseContext.FASTING, imperial) != null)
        assertNull(glucosePreview("", GlucoseContext.FASTING, metric))
        assertNull(glucosePreview("0", GlucoseContext.FASTING, metric))
        assertNull(glucosePreview("500", GlucoseContext.FASTING, metric))
    }
}
